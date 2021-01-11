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

(defn- add-to-index
  [user url content]
  (let [index (get index-cache user (clucy/memory-index))]
    (clucy/add index
               {:user user
                :url url
                :content content})))

(defn- encrypt-pw
  [pw]
  (password/encrypt pw))

(defn add-bookmarks
  [user urls]
  (map #(add-to-index user % (extract-text %)) urls))

(defn add-user
  [user]
  ;; todo refactor to a 'some? -> blank? v'
  (if (or (str/blank? (:email user))
          (str/blank? (:password user)))  
    (response/bad-request "email or password missing")
    ;; todo use a 201, location -> GET /user endpoint
    (response/response (db/create-user (update user :password encrypt-pw)))))

