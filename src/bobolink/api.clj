(ns bobolink.api
  (:import (org.jsoup Jsoup))
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

(comment (defn add-bookmarks
           [user urls]
           (map #(add-to-index user % (extract-text %)) urls)))

(defn add-bookmarks
  [req]
  ;; update indexes. both remote and in cache
  (response/response (db/add-bookmarks req)))

(defn get-bookmarks
  [userid]
  (response/response (map :url (db/get-bookmarks userid))))

(defn- encrypt-pw
  [pw]
  (password/encrypt pw))

(defn- valid-creds?
  [creds]
  (= creds (-> (:email creds)
               (db/get-user-full)
               (select-keys [:email :password]))))

(defn authenticated?
  [userid token]
  (= token (:authtoken (db/get-auth-token userid))))

(defn gen-token
  [auth]
  (response/response auth))

(defn get-user
  [userid]
  (response/response (db/get-user {:userid userid})))

(defn add-user
  [user]
  (if (not-any? str/blank? [(:email user) (:password user)])    
    (let [user (db/create-user (update user :password encrypt-pw))]
      (response/created (str "/users/" (:id user))))
    (response/bad-request "email or password missing")))


