(ns bobolink.search
  (:import (org.jsoup Jsoup))
  (:require [clojure.string :as str]
            [clucy.core :as clucy]))

;; tmp map cache
(def index-cache {})

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

(defn extract-text
  [urls]
  (map #(.text (.body (Jsoup/parse (slurp %)))) urls))

(defn update-store
  [bookmarks]
  (prn "added bookmarks"))
