(ns bobolink.lucene
  (:import (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.document Document Field FieldType Field$Store)
           (org.apache.lucene.index DirectoryReader IndexReader IndexOptions
                                    IndexWriter IndexWriterConfig Term)
           (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.store FSDirectory NIOFSDirectory)
           (org.apache.lucene.search IndexSearcher BooleanQuery TermQuery
                                     BooleanClause$Occur BooleanQuery$Builder)
           (java.nio.file Paths))
  (:require [clojure.java.io :as io]))

(def analyzer (StandardAnalyzer.))

(defn disk-index
  "Creates or opens a lucene index at the provided `file` path."
  [file]
  (FSDirectory/open (Paths/get (.toURI file))))

(defn- entry->field
  [e]
  (Field. (name (key e))
          (str (val e))
          (doto (FieldType.)
            (.setStored true)
            (.setIndexOptions IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS))))

(defn- map->doc
  [m]
  (let [fields (for [e m] (entry->field e))
        doc (Document.)]
    (doseq [f fields]
      (.add doc f))
    doc))

(defn add!
  "Adds documents to a lucene index.
  Excpects `docs` to be a coll of maps with keys denoting fields and values content."
  [index docs]
  (with-open [writer (IndexWriter. index (IndexWriterConfig. analyzer))]
    (let [lu-docs (map map->doc docs)]
      (doseq [d lu-docs]
        (.addDocument writer d)))))

(defn delete!
  "Deletes documents from a lucene index.
  Expects `maps` to be a coll of maps with keys denoting fields and values content."
  [index maps]
  (with-open [writer (IndexWriter. index (IndexWriterConfig. analyzer))]
    (doseq [m maps]
      (let [builder (BooleanQuery$Builder.)]
        (doseq [[k v] m]
          (.add builder (TermQuery. (Term. (name k) v)) BooleanClause$Occur/MUST))
        (.deleteDocuments writer (into-array (vector (.build builder))))))))

(defn- doc->map
  [d]
  (into {} (map #(vector (.name %) (.stringValue %)) (.getFields d))))

(defn search
  "Searches a lucene index against the provided `query`.
  Always uses lucene's default QueryParser for search."
  ([index query]
   (search index query "content"))
  ([index query field]
   (with-open [reader (DirectoryReader/open index)]
    (let [searcher (IndexSearcher. reader)
          parser (QueryParser. field analyzer)
          query (.parse parser query)
          hits (.search searcher query Integer/MAX_VALUE)]
      (doall
       (for [hit (map (partial aget (.scoreDocs hits)) (range (.value (.totalHits hits))))]
         (->> (.doc hit)
             (.doc searcher)
             (doc->map))))))))


