(ns bobolink.search
  (:import (org.jsoup Jsoup))
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clucy.core :as clucy]))


(comment (defn add-bookmarks
           [user urls]
           (map #(add-to-index user % (extract-text %)) urls)))

(defn- extract-text
  [url]
  (-> url slurp Jsoup/parse .body .text (str/split #" ") set))

(def stop-words (-> "stop-words" io/resource slurp str/split-lines set))

(defn- remove-stopwords
  [text]
  (clojure.set/difference text stop-words))

(defn- get-content
  [url]
  (-> url extract-text remove-stopwords))

(defn- add-to-index
  [index bookmark]
  (clucy/add index (update bookmark :user #(apply (partial dissoc %) #{:email :password}))))

(defn- fetch-index
  [user]
  (clucy/memory-index))

(defn gen-bookmark
  [user url]
  (hash-map :user user :url url :content (get-content url)))

(defn update-store
  [bookmarks]
  (let [{:keys [user]} (first bookmarks)
        index (fetch-index user)]
    (doseq [bookmark bookmarks] (add-to-index index bookmark))
    index))

(comment (clucy/search
  (update-store [(gen-bookmark {:id 1 :email "j@a.com"} "https://en.wikipedia.org/wiki/2021_Russian_protests")])
  "function" 10 :default-field :content))
