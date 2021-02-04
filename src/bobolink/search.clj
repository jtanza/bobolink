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
  "Removes common english stopwords from the provided `text`."
  [text]
  (clojure.set/difference text stop-words))

(defn- extract-text
  "Attempts to download the HTML located at `url` returning all text in the document."
  [url]
  (-> url slurp Jsoup/parse .body .text (str/split #" ") set))

(defn- get-content
  "Downloads and massages the text located at `url` for use as the content in a user's stored bookmark."
  [url]
  (->> url extract-text remove-stopwords (str/join " ")))

(defn gen-bookmark
  [user url]
  (if-not (empty? url) (hash-map :user user :url url :content (get-content url))))

(def index-cache
  "In memory cache holding our active Lucene indexes."
  (cache/lru-cache-factory {}))

(def analyzer (clucie-analysis/standard-analyzer))

(defn- run-at-interval
  [f interval]
  (future (while true
            (do (Thread/sleep interval)
                (try (f)
                     (catch Exception e
                       (prn e)))))))

(defn- delete-dir
  "Recursively deletes the directory located at `file`."
  [file]
  (when (.isDirectory file)
    (doseq [f (.listFiles file)]
      (delete-dir f)))
  (io/delete-file file))

(defn- delete-stale-indexes []
  "Purges disk of indexes that were once held in our `[[index-cache]]` but have since been evicted."
  (let [disk-indexes (into {} (map #(let [path (.toString %)]
                                      (-> (subs path (inc (str/last-index-of path "/")) (count path))
                                          (keyword)
                                          (vector %)))
                                   (.listFiles (io/file "./index/"))))]
    (doseq [id (keys disk-indexes)]
      (print id)
      (when-not (cache/has? index-cache id)
        (delete-dir (get disk-indexes id))))))

(def index-reaper
  "Runs `[[delete-stale-indexes]]` in a background thread every twenty minutes."
  (run-at-interval delete-stale-indexes 60000))

(defn- add-to-index
  "Adds a `bookmark` to a users associated Lucene `index`."
  [index bookmark]
  (clucie-core/add! index
                    (vector (update bookmark :user #(apply (partial dissoc %) #{:email :password})))
                    (keys bookmark)
                    analyzer))

(defn- build-index
  "Attempts to build a Lucene index from a `user`s saved bookmarks."
  [user]
  (let [dir (io/file (str "./index/" (:id user)))]
    (when (.exists dir)
      (delete-dir dir))
    (let [index (clucie-store/disk-store dir)
        bookmarks (map (partial gen-bookmark user) (db/get-bookmarks user))]
    (doseq [bookmark bookmarks] (add-to-index index bookmark))
    index)))

(defn- ensure-parent-dir
  [path]
  (let [parent-dir (io/file (subs path 0 (str/last-index-of path "/")))]
    (if-not (.exists parent-dir)
      (.mkdirs parent-dir))
    (io/file parent-dir)))

(defn- uncompress-index
  "Inflates a zipped directory in the form of an `input-stream` to the provided `dest`."
  [input-stream dest]
  (with-open [zip-stream (ZipInputStream. input-stream)]
      (loop [entry (.getNextEntry zip-stream)]
        (if entry
          (let [path (str dest (.getName entry))
                file (io/file path)]
            (if (.isDirectory entry)
              (if-not (.exists file)
                (.mkdirs file))
              (when (ensure-parent-dir path)
                (io/copy zip-stream file)))
            (recur (.getNextEntry zip-stream)))))
    (io/file dest)))

(defn- user->key
  [user]
  (str (:id user) "-index.zip"))

(defn- fetch-index-remote
  "Attempts to download a `user`s associated Lucene index stored on AWS S3."
  [user]
  (try
    (-> (s3/get-object {:bucket-name "bobo-index" :key (user->key user)})
        (:input-stream)
        (uncompress-index (str "./index/" (:id user) "/"))
        (.getAbsolutePath)
        (clucie-store/disk-store))
    (catch Exception e
      (let [m (amazonica.core/ex->map e)]
        (when-not  (= 404 (:status-code m)) ;; swallow 404s as theyre expected for new users
          (prn amazonica.core/ex->map e))))))

(defn- user->cache-key
  [user]
  (keyword (str (:id user))))

(defn- get-index
  "Retrieves a `user`s associated index from the `[[index-cache]]`.
  On a cache miss will first attempty to fetch a remote index on S3. If no such index
  exists, will build an index directly with a user's saved bookmarks."
  [user]
  (cache/lookup-or-miss index-cache (user->cache-key user)
                        (fn [_] (let [remote-index (fetch-index-remote user)]
                                 (if (nil? remote-index) (build-index user) remote-index)))))

(defn- compress-index
  [index user]
  (let [path (.toString (.getDirectory index))
        dest (str "/tmp/index/" (:id user) "-index.zip")]
    (when (ensure-parent-dir dest)
      (with-open [zip (ZipOutputStream. (io/output-stream dest))]
      (doseq [f (file-seq (io/file path)) :when (.isFile f)]
        (.putNextEntry zip (ZipEntry. (str/replace-first (.getPath f) path "")))
        (io/copy f zip)
        (.closeEntry zip))))
    (io/file dest)))

(defn- update-store
  "Synchronizes a `user`s remote index stored on S3 with the provided `index`."
  [user index]
  (let [index-file (compress-index index user)]
    (s3/put-object {:bucket-name "bobo-index"
                    :key (user->key user)
                    :file (compress-index index user)})
    (io/delete-file index-file true)))

(defn save-bookmarks
  "Adds `bookmarks` to a user's saved bookmarks collection.
  Also updates the `index-cache` and the user's remote index on S3."
  [bookmarks]
  (let [{:keys [user]} (first bookmarks)
        index (get-index user)]
    (doseq [bookmark bookmarks] (add-to-index index bookmark))
    (update-store user index)
    index))

