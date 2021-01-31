(ns bobolink.search
  (:import (org.jsoup Jsoup))
  (:require [clojure.core.cache.wrapped :as cache]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cognitect.aws.client.api :as aws]
            [clucy.core :as clucy]))

(def s3 (aws/client {:api :s3}))

(aws/validate-requests s3 true)

; (def idx (aws/invoke s3 {:op :GetObject :request {:Bucket "bobo-index" :Key "index-one"}}))
; (slurp (:Body idx))

(def index-cache (cache/lru-cache-factory {}))

(defn- user->key
  [user]
  (str (:id user)))

(defn- fetch-index
  [user]
  (let [s3-req (aws/invoke s3 {:op :GetObject :request {:Bucket "bobo-index" :Key (user->key user)}})
        val (if (= :cognitect.anomalies/not-found
                   (:cognitect.anomalies/category s3-req))
              (clucy/memory-index)
              (:Body s3-req))]
    (cache/lookup-or-miss index-cache (:id user) (constantly val))))

(defn- add-to-index
  [index bookmark]
  (clucy/add index (update bookmark :user #(apply (partial dissoc %) #{:email :password}))))

(defn- update-store
  [user index]
  (aws/invoke s3 {:op :PutObject :request {:Bucket "bobo-index" :Key (user->key user)
                                           :Body (.getBytes index)}}))

(defn save-bookmarks
  [bookmarks]
  (let [{:keys [user]} (first bookmarks)
        index (fetch-index user)]
    (doseq [bookmark bookmarks] (add-to-index index bookmark))
    (update-store user index)
    index))

(def stop-words (-> "stop-words" io/resource slurp str/split-lines set))

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

