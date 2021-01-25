(ns bobolink.api
  (:import (org.jsoup Jsoup)
           (java.security SecureRandom)
           (java.util Base64))
  (:require [bobolink.db :as db]
            [clojure.string :as str]
            [clucy.core :as clucy]            
            [crypto.password.bcrypt :as password]
            [ring.util.response :as response]))

;; tmp map cache
(def index-cache {})

;; TODO handle bad urls
(defn- extract-text
  [urls]
  (map #(.text (.body (Jsoup/parse (slurp %)))) urls))

;; TODO write through cache
(defn- add-to-index
  [user url content]
  (let [index (get index-cache user (clucy/memory-index))]
    (clucy/add index
               {:user user
                :url url
                :content content})))

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
  [id]
  (response/response (db/get-user {:id id})))

(defn add-user
  [creds]
  (if (not-any? str/blank? [(:email creds) (:password creds)])
    (if-let [user (db/create-user (update creds :password password/encrypt))]
      (response/created (str "/users/" (:id user))))
    (response/bad-request "email or password missing")))

(defn get-bookmarks
  [username]
  (response/response (map :url (db/get-bookmarks username))))

(comment (defn add-bookmarks
           [user urls]
           (map #(add-to-index user % (extract-text %)) urls)))

(defn add-bookmarks
  [username urls]
  ;; update indexes. both remote and in cache
  (response/response (db/add-bookmarks (db/get-user {:email username}) urls)))

