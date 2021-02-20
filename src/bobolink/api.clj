(ns bobolink.api
  (:import (org.jsoup Jsoup)
           (java.security SecureRandom)
           (java.util Base64))
  (:require [bobolink.db :as db]
            [bobolink.search :as search]
            [clojure.string :as str]
            [crypto.password.bcrypt :as password]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre
             :refer [debug info]]))

(defn- gen-token []
  "Generates a \"random\" API authtoken."
  (let [rndm (SecureRandom.)
        base64 (.withoutPadding (Base64/getUrlEncoder))
        buffer (byte-array 32)]
    (.nextBytes rndm buffer)
    (.encodeToString base64 buffer)))

(defn- user-from-creds
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
    (not (re-matches #"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$" (:email creds))) "invalid email address"
    (not (re-matches #"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]*)[A-Za-z\d@$!%*?&]{8,}$" (:password creds)))
    "password must be minimum eight characters, have at least one uppercase letter, one lowercase letter, and one number"
    (seq (db/get-user (update creds :email str/lower-case))) "username taken"))

(defn add-user
  "Creates a new bobolink user."
  [creds]
  (if-some [invalid-reason (validate-creds creds)]
    (response/bad-request invalid-reason)
    (let [user (db/create-user (-> creds
                                   (update :password password/encrypt)
                                   (update :email str/lower-case)))]
      (response/created (str "/users/" (:id user))))))

(defn get-bookmarks
  [userid]
  (response/response (map :url (db/get-bookmarks {:id (Integer/parseInt userid)}))))

(def ^:private resp {:headers {}})

(defn add-bookmarks
  "Adds bookmarks from a list of request `urls` to a user's collection."
  [username urls]
  (if (< 50 (count urls))
    (response/bad-request "Please add bookmarks in batches of 50 or less.")
    (try
      (let [user (db/get-user {:email username})
            bookmarks (map (partial search/gen-bookmark user) urls)
            valid-urls (map :url bookmarks)]
        (if (seq valid-urls)          
          (do (db/save-bookmarks user valid-urls)
              (search/save-bookmarks user bookmarks)
              ;; TODO created?
              (response/response valid-urls))
          (response/bad-request "Could not gather content from bookmarks")))
      (catch Exception e
        (debug e)
        (response/status (assoc resp :body (str "Error adding bookmarks: " (.getMessage e))) 500)))))

(defn delete-bookmarks
  [username urls]
  (try
    (let [user (db/get-user {:email username})          
          to-delete (clojure.set/intersection (set urls) (set (map :url (db/get-bookmarks user))))]
      (if (seq to-delete)
        (do (db/delete-bookmarks user to-delete)
            (search/delete-bookmarks user to-delete)
            (response/response to-delete))        
        (response/bad-request "Cannot delete; bookmarks don't exist")))
    (catch Exception e
      (debug e)
      (response/status (assoc resp :body (str "Error deleting bookmarks: " (.getMessage e))) 500))))

(defn search-bookmarks
  [username search-req]
  (try
    (response/response
     (search/search-bookmarks (db/get-user {:email username}) search-req))
    (catch Exception e
      (debug e)
      (response/status (assoc resp :body (str "Error searching bookmarks: " (.getMessage e))) 500))))

