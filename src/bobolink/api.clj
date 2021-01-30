(ns bobolink.api
  (:import (org.jsoup Jsoup)
           (java.security SecureRandom)
           (java.util Base64))
  (:require [bobolink.db :as db]
            [bobolink.search :as search]
            [clojure.string :as str]
            [clucy.core :as clucy]            
            [crypto.password.bcrypt :as password]
            [ring.util.response :as response]))

(defn- gen-token []
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
  [email token]
  (= token (-> {:email email}
               (db/get-user)
               (db/get-auth-token)
               (:authtoken))))

(defn get-token
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
  [creds]
  (if (not-any? str/blank? [(:email creds) (:password creds)])
    (if-let [user (db/create-user (update creds :password password/encrypt))]
      (response/created (str "/users/" (:id user))))
    (response/bad-request "email or password missing")))

(defn get-bookmarks
  [userid]
  (response/response (map :url (db/get-bookmarks {:id (Integer/parseInt userid)}))))

(defn add-bookmarks
  [username urls]
  (if (< 50 (count urls))
    (response/bad-request "Exceeded 50 bookmark limit")
    (try
      (let [bookmarks (map #(hash-map :url % :content (search/extract-text %)) urls)
            valid-urls (map :url bookmarks)]
        (if (seq valid-urls)
          (do (db/add-bookmarks (db/get-user {:email username}) valid-urls)
              (search/update-store bookmarks)
              (response/response valid-urls))
          (response/bad-request "Could not gather content from bookmarks")))
      (catch Exception e (response/bad-request (str "Error adding bookmarks: " (.getMessage e)))))))

