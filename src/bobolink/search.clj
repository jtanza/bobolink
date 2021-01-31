(ns bobolink.search
  (:import (org.jsoup Jsoup))
  (:require [clojure.core.cache.wrapped :as cache]
            [clojure.string :as str]
            [clucy.core :as clucy]))

(def index-cache (cache/lru-cache-factory {}))

(defn- fetch-index
  [user]
  (cache/lookup-or-miss index-cache (:id user) (constantly (clucy/memory-index))))

(defn- add-to-index
  [index bookmark]
  (clucy/add index (update bookmark :user #(apply (partial dissoc %) #{:email :password}))))

(defn update-store
  [bookmarks]
  (let [{:keys [user]} (first bookmarks)
        index (fetch-index user)]
    (doseq [bookmark bookmarks] (add-to-index index bookmark))
    index))

(def stop-words (-> "stop-words" clojure.java.io/resource slurp str/split-lines set))

(defn- remove-stopwords
  [text]
  (clojure.set/difference text stop-words))

(defn- extract-text
  [url]
  (-> url slurp Jsoup/parse .body .text (str/split #" ") set))

(defn- get-content
  [url]
  (->> url extract-text remove-stopwords (str/join " ")))

(defn gen-bookmark
  [user url]
  (hash-map :user user :url url :content (get-content url)))

(clucy/search
 (update-store [(gen-bookmark {:id 1 :email "j@a.com"} "https://en.wikipedia.org/wiki/2021_Russian_protests")])
 "russian" 10 :default-field :content)


