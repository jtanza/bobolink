(ns bobolink.api
  (:import (org.jsoup Jsoup)
           (java.security SecureRandom)
           (java.util Base64))
  (:require [bobolink.db :as db]
            [clojure.string :as str]
            [clucy.core :as clucy]            
            [crypto.password.bcrypt :as password]
            [ring.util.response :as response]))

(def index-cache {}) ;; tmp map cache

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

(defn- valid-creds?
  [creds]
  (let [user (db/get-user-full (:email creds))]
    (password/check (:password creds) (:password user))))

(defn- decode
  [x coll]
  (apply assoc {}
         (interleave coll (str/split (String. (.decode (Base64/getDecoder) x)) #":"))))

(defn authenticated?
  [userid token]
  (= token (:authtoken (db/get-auth-token userid))))

(defn get-token
  [auth]
  (if (valid-creds? (decode auth [:email :password]))
      (response/response (gen-token))
      (response/bad-request "could not validate user")))

(defn get-user
  [userid]
  (response/response (db/get-user {:userid userid})))

(defn add-user
  [user]
  (if (not-any? str/blank? [(:email user) (:password user)])    
    (let [user (db/create-user (update user :password password/encrypt))]
      (response/created (str "/users/" (:id user))))
    (response/bad-request "email or password missing")))

(defn get-bookmarks
  [userid]
  (response/response (map :url (db/get-bookmarks userid))))

(comment (defn add-bookmarks
           [user urls]
           (map #(add-to-index user % (extract-text %)) urls)))

(defn add-bookmarks
  [req]
  ;; update indexes. both remote and in cache
  (response/response (db/add-bookmarks req)))

