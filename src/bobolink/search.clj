(ns bobolink.search
  (:import (org.jsoup Jsoup)
           (java.util.zip ZipEntry ZipOutputStream ZipInputStream)
           (org.apache.lucene.search.highlight Highlighter QueryScorer
                                               SimpleHTMLFormatter)
           (com.amazonaws AmazonServiceException))
  (:require [bobolink.db :as db]
            [bobolink.lucene :as lucene]
            [amazonica.aws.s3 :as s3]
            [amazonica.core :as amazonica]
            [clojure.core.cache.wrapped :as cache]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [taoensso.timbre :as timbre
             :refer [debug info]]))

(defn- extract-text
  "Attempts to download the HTML located at `url` returning all text in the document."
  [url]
  (-> url slurp Jsoup/parse (.select "p") .text (str/replace #"[^a-zA-Z0-9 -]" "")))

(defn gen-bookmark
  [user url]
  (if-not (empty? url) (hash-map :userid (:id user) :url url :content (extract-text url))))

(def ^:private index-cache
  "In memory cache holding our active Lucene indexes."
  (cache/lru-cache-factory {}))

(defn- delete-dir
  "Recursively deletes the directory located at `file`."
  [file]
  (when (.isDirectory file)
    (doseq [f (.listFiles file)]
      (delete-dir f)))
  (io/delete-file file))

(def ^:private index-dir "/opt/bobolink/index/")

(defn- build-index
  "Attempts to build a Lucene index from a `user`s saved bookmarks."
  [user]
  (let [dir (io/file (str index-dir (:id user)))]
    (when (.exists dir)
      (delete-dir dir))
    (let [index (lucene/disk-index dir)]
      (lucene/add! index (map (partial gen-bookmark user) (map :url (db/get-bookmarks user))))
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
    (slurp input-stream) ;; TODO ensure the whole stream gets consumed/closed
    (.getAbsoluteFile (io/file dest))))

(defn- user->key
  "Constructs an index object key for use with s3."
  [user]
  (let [id (:id user)]
    (str id "/" id "-index.zip")))

(defn- fetch-index-remote
  "Attempts to download a `user`s associated Lucene index stored on AWS S3."
  [user]
  (info (str "fetching remote index for user: " (:id user)))
  (try
    (-> (s3/get-object {:bucket-name "bobo-index" :key (user->key user)})
        (:input-stream)
        (uncompress-index (str index-dir (:id user) "/"))
        (lucene/disk-index))
    (catch AmazonServiceException e
      (let [m (amazonica/ex->map e)]
        (when-not (= 404 (:status-code m)) ;; swallow 404s as theyre expected for new users
          (debug amazonica/ex->map e))))
    (catch Exception e
      (debug e))))

(defn- user->cache-key
  [user]
  (keyword (str (:id user))))

(defn- get-index
  "Retrieves a `user`s associated index from the `[[index-cache]]`.
  On a cache miss will first attempt to fetch a remote index on S3. If no such index
  exists, will build an index directly with a user's saved bookmarks, add it to 
  the cache and return it to the calling function."
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

(defn- sync-store
  "Synchronizes a `user`s remote index stored on S3 with the provided `index`."
  [user index]
  (let [index-file (compress-index index user)]
    (s3/put-object {:bucket-name "bobo-index"
                    :key (user->key user)
                    :file (compress-index index user)})
    (info (str "wrote index file to remote for user " (:email user)))
    (io/delete-file index-file true)))

(defn save-bookmarks
  "Adds `bookmarks` to a user's `index-cache` along with their remote store on S3."
  [user bookmarks]
  (let [index (get-index user)]
    (lucene/add! index bookmarks)
    (sync-store user index)
    index))

(defn delete-bookmarks
  "Removes the bookmarks corresponding to the provided `urls` from a
  user's `index-cache` along with their remote store on S3."
  [user urls]
  (let [{userid :id} user
        index (get-index user)]
    (info (str "deleting " urls " for user " userid))
    (lucene/delete! index (map #(hash-map :id %) urls))
    (sync-store user index)))

(defn search-bookmarks
  "Searches a `user`s saved bookmarks against `search-req`, applying `f` to the coll of search hits.
  Returns raw results when no `f` is supplied."
  ([user search-req]
   (search-bookmarks user search-req #(map identity %)))
  ([user search-req f]
   (f (lucene/search (get-index user) search-req))))

(defn- run-at-interval
  "Executes `f` in a background thread at the provided `interval` in ms."
  [f interval]
  (future (while true
            (do (Thread/sleep interval)
                (try (f)
                     (catch Exception e
                       (debug e)))))))

(defn- delete-stale-indexes
  "Purges disk of indexes that were once held in our `[[index-cache]]` but have since been evicted."
  []
  (info "pruning stale indexes...")
  (let [disk-indexes (into {} (map #(let [path (.toString %)]
                                      ;; grab userid from index path
                                      (-> (subs path (inc (str/last-index-of path "/")) (count path))
                                          (keyword)
                                          (vector %)))
                                   (.listFiles (io/file index-dir))))]
    (doseq [id (keys disk-indexes)]
      (when-not (cache/has? index-cache id)
        (info (str "deleting stale index: " id))
        (delete-dir (get disk-indexes id))))))

(def ^:private index-reaper
  "Runs `[[delete-stale-indexes]]` in a background thread every ten minutes."
  (run-at-interval delete-stale-indexes (* 1000 60 10)))
