(ns bobolink.api
  (:import (org.jsoup Jsoup)
           (java.security SecureRandom)
           (java.util Base64))
  (:require [bobolink.db :as db]
            [bobolink.search :as search]
            [clojure.string :as str]
            [crypto.password.bcrypt :as password]
            [ring.util.response :as response]))

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
  "Returns a user's stored authtoken."
  [creds]
  (if-let [user (user-from-creds creds)]
    (let [token (gen-token)]
      (db/set-auth-token user token)
      (response/response token))
    (response/bad-request "could not validate user")))

(defn get-user
  [user]
  (response/response (db/get-user user)))

(defn add-user
  "Creates a new bobolink user."
  [creds]
  (if (not-any? str/blank? [(:email creds) (:password creds)])
    (if-let [user (db/create-user (update creds :password password/encrypt))]
      (response/created (str "/users/" (:id user))))
    (response/bad-request "email or password missing")))

(defn get-bookmarks
  [userid]
  (response/response (map :url (db/get-bookmarks {:id (Integer/parseInt userid)}))))

(defn add-bookmarks
  "Adds bookmarks from a list of request `urls` to a user's collection."
  [username urls]
  (if (< 50 (count urls))
    (response/bad-request "Exceeded 50 bookmark limit")
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
      (catch Exception e (response/bad-request (str "Error adding bookmarks: " (.getMessage e)))))))

(defn delete-bookmarks
  [username urls]
  (try
    (let [user (db/get-user {:email username})]
      (do (db/delete-bookmarks user urls)
          (search/delete-bookmarks user urls)
          ;; TODO return delete urls/ gone?
          (response/response urls)))
    ;; TODO logging
    (catch Exception e
      (prn e)
      (response/bad-request (str "error " (.getMessage e))))))

(defn search-bookmarks
  [username search-req]
  (try
    (response/response
     (search/search-bookmarks (db/get-user {:email username}) search-req))
    ;; TODO logging
    (catch Exception e (response/bad-request (str "Error searching bookmarks: " (.getMessage e))))))
