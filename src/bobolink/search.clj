(ns bobolink.search
  (:import (org.jsoup Jsoup)
           (java.util.zip ZipEntry ZipOutputStream ZipInputStream))
  (:require [bobolink.db :as db]
            [amazonica.aws.s3 :as s3]
            [clojure.core.cache.wrapped :as cache]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clucie.core :as clucie-core]
            [clucie.analysis :as clucie-analysis]
            [clucie.store :as clucie-store]))

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
  (if-not (empty? url) (hash-map :user user :url url :content (get-content url))))

;; TODO on eviction we'll have to clean up the disk space used by the file directory
(def index-cache (cache/lru-cache-factory {}))

(def analyzer (clucie-analysis/standard-analyzer))

(defn- user->key
  [user]
  (str (:id user) "-index.zip"))

(defn- add-to-index
  [index bookmark]
  (clucie-core/add! index
                    (vector (update bookmark :user #(apply (partial dissoc %) #{:email :password})))
                    (keys bookmark)
                    analyzer))

(defn- build-index
  [user]
  (let [index (clucie-store/disk-store (str "./index/" (:id user)))
        bookmarks (map (partial gen-bookmark user) (db/get-bookmarks user))]
    (doseq [bookmark bookmarks] (add-to-index index bookmark))
    index))

(defn- uncompress-index
  [input-stream dest]
  (with-open [zip-stream (ZipInputStream. input-stream)]
      (loop [entry (.getNextEntry zip-stream)]
        (if entry
          (let [save-path (str dest (.getName entry))
                save-file (io/file save-path)]
            (if (.isDirectory entry)
              (if-not (.exists save-file)
                (.mkdirs save-file))
              (let [parent-dir (io/file (.substring save-path 0 (.lastIndexOf save-path "/")))]
                (if-not (.exists parent-dir) (.mkdirs parent-dir))
                (clojure.java.io/copy zip-stream save-file)))
            (recur (.getNextEntry zip-stream)))))
    (io/file dest)))

(defn- fetch-index-remote
  [user]
  (try
    (-> (:input-stream (s3/get-object {:bucket-name "bobo-index" :key (user->key user)}))
        (uncompress-index (str "./index/" (:id user) "/"))
        (.getAbsolutePath)
        (clucie-store/disk-store))
    (catch Exception e
      (prn amazonica.core/ex->map e))))

(defn- get-index
  [user]
  (cache/lookup-or-miss index-cache (:id user)
                        (fn [_] (let [remote-index (fetch-index-remote user)]
                                 (if (nil? remote-index) (build-index user) remote-index)))))

(defn- compress-index
  [index user]
  (let [path (.toString (.getDirectory index))
        name (str "/tmp/" (:id user) "-index.zip")]
    (with-open [zip (ZipOutputStream. (io/output-stream name))]
      (doseq [f (file-seq (io/file path)) :when (.isFile f)]
        (.putNextEntry zip (ZipEntry. (str/replace-first (.getPath f) path "")))
        (io/copy f zip)
        (.closeEntry zip)))
    (io/file name)))

(defn- update-store
  [user index]
  (s3/put-object {:bucket-name "bobo-index"
                  :key (user->key user)
                  :file (compress-index index user)}))

(defn save-bookmarks
  [bookmarks]
  (let [{:keys [user]} (first bookmarks)
        index (get-index user)]
    (doseq [bookmark bookmarks] (add-to-index index bookmark))
    (update-store user index)
    index))

