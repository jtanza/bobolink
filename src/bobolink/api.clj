(ns bobolink.api
  (:import (org.jsoup Jsoup)
           (java.security SecureRandom)
           (java.util Base64))
  (:require [bobolink.db :as db]
            [bobolink.search :as search]
            [bobolink.util :as util]
            [clojure.string :as str]
            [clojure.set :refer [intersection]]
            [crypto.password.bcrypt :as password]
            [postal.core :as postal]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre
             :refer [debug info]]))

(defn- gen-token
  "Generates a \"random\" API authtoken."
  []
  (let [rndm (SecureRandom.)
        base64 (.withoutPadding (Base64/getUrlEncoder))
        buffer (byte-array 32)]
    (.nextBytes rndm buffer)
    (.encodeToString base64 buffer)))

(defn- user-from-creds
  "Returns the valid user represented by `creds`, otherwise nil"
  [creds]
  (if-let [user (db/get-user-full (:email creds))]
    (when (password/check (:auth creds) (:password user))
      (dissoc user :password))))

(defn authenticated?
  "Determines whether the provided `token` matches a user's stored authtoken."
  [email token]
  (= token (-> {:email email}
               (db/get-user)
               (db/get-auth-token)
               (:authtoken))))

(defn get-token
  "Creates, stores and returns an authentication token."
  [creds]
  (if-let [user (user-from-creds creds)]
    (let [token (gen-token)]
      (db/set-auth-token user token)
      (response/response token))
    (response/bad-request "could not validate user")))

(defn get-user
  [user]
  (response/response (db/get-user user)))

(defn- validate-creds
  "Validates new user requests.
  Returns the reason phrase for denied requests, otherwise nil."
  [creds]
  (cond
    (some str/blank? [(:email creds) (:password creds)]) "email or password missing"
    ;; https://xkcd.com/208/
    (not (re-matches #"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$" (:email creds))) "invalid email address"
    (not (re-matches #"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]*)[A-Za-z\d@$!%*?&]{8,}$" (:password creds)))
    "password must be minimum eight characters, have at least one uppercase letter, one lowercase letter, and one number"
    (and (:new-user creds) (seq (db/get-user (update creds :email str/lower-case)))) "username taken"))

(defn add-user
  "Creates a new bobolink user.
  Callers can provide an `add` and `format-resp` function if they would like to persist the validated
  user in a manner other than `db/create-user`"
  ([creds]
   (add-user (assoc creds :new-user true) db/create-user #(response/created (str "/users/" (:id %)))))
  ([creds add format-resp]
   (if-some [invalid-reason (validate-creds creds)]
    (response/bad-request invalid-reason)
    (let [user (add (-> creds
                        (update :password password/encrypt)
                        (update :email str/lower-case)
                        (select-keys [:email :password :id])))]
      (format-resp user)))))

(defn get-bookmarks
  [userid]
  (response/response (map :url (db/get-bookmarks {:id (Integer/parseInt userid)}))))

(def ^:private resp {:headers {}})

(defn add-bookmarks
  "Adds bookmarks from a list of request `urls` to a user's collection."
  [username urls]
  (let [user (db/get-user {:email username})
        existing (count (db/get-bookmarks user))]
    (if (< 250 (+ existing (count urls)))
      (response/bad-request (str "Unable to add bookmarks as you currently have " existing " saved bookmarks."
                                 "As there is a limit of 250 bookmarks per user, please lower your requested amount or"
                                 "reach out to support@bobolink.com for information on how to lift these limits."))
      (try
        (let [bookmarks (map (partial search/gen-bookmark user) urls)
              valid-urls (map :url bookmarks)]
          (if (seq valid-urls)
            (do (db/save-bookmarks user valid-urls)
                (search/save-bookmarks user bookmarks)
                (response/response valid-urls))
            (response/bad-request "Could not gather content from bookmarks")))
        (catch Exception e
          (debug e)
          (response/status (assoc resp :body (str "Error adding bookmarks: " (.getMessage e))) 500))))))

(defn delete-bookmarks
  [username urls]
  (try
    (let [user (db/get-user {:email username})          
          to-delete (intersection (set urls) (set (map :url (db/get-bookmarks user))))]
      (if (seq to-delete) ;; don't bother deleting bookmarks that don't exist
        (do (db/delete-bookmarks user to-delete)
            (search/delete-bookmarks user to-delete)
            (response/response to-delete))        
        (response/bad-request "Cannot delete; bookmarks don't exist")))
    (catch Exception e
      (debug e)
      (response/status (assoc resp :body (str "Error deleting bookmarks: " (.getMessage e))) 500))))

(defn search-bookmarks
  "Searches a user's stored bookmarks against the provided `search-req`"
  [username search-req]
  (try
    (response/response
     (search/search-bookmarks (db/get-user {:email username}) search-req))
    (catch Exception e
      (debug e)
      (response/status (assoc resp :body (str "Error searching bookmarks: " (.getMessage e))) 500))))

(defn- send-mail
  ([content]
   (send-mail content "no-reply@bobolink.me"))
  ([content from]
   (let [{:keys [to subject body]} content]
    (info (str "sending email to: " to))
    (postal/send-message {:host "email-smtp.us-east-1.amazonaws.com"
                          :user (:smtp-username util/conf)
                          :pass (:smtp-password util/conf)
                          :port 587 :tls true}
                         {:from from :to to
                          :subject subject :body body}))))

(defmulti reset-password
  (fn [req] (sort (keys req))))

(defmethod reset-password '(:email) [req]
  (if-some [user (db/get-user (select-keys req [:email]))]
    (try
      (let [email (:email user)
            start (rand-int 25)
            token (subs (gen-token) start (+ 15 start))
            send-resp (send-mail {:to email :subject "Bobolink Password Reset Request"
                                  :body (str "Hi There!\n\nHere's your password reset token: " token
                                             "\n\nIf you didn't request this token, you can safely ignore this email "
                                             "as your password will remain unchanged if no further action is taken.")})]
        (do (db/set-reset-token user token)
            (response/status (assoc resp :body (str "Email sent to " email)) 202)))
      (catch Exception e
        (debug e)
        (response/status (assoc resp :body (str "Error sending mail: " (.getMessage e))) 500)))
    (response/not-found "Not Found")))

(defmethod reset-password '(:email :new :token) [req]
  (let [user (db/get-user (select-keys req [:email]))
        reset-token (db/get-reset-token user)]
    (if (and (seq reset-token) (= reset-token (select-keys req [:token])))
      (do (db/destroy-auth-token user)
          (db/destroy-reset-token user)
          (add-user (assoc user :password (:new req)) db/update-user (constantly (response/status 204))))
      (response/bad-request "Invalid reset token"))))

(defmethod reset-password :default [req]
  (response/bad-request (str "Invalid fields: " (seq (map name (keys req))))))

