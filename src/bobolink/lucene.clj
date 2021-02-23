
(ns bobolink.lucene
  (:import (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.analysis.standard StandardAnalyzer)
           (org.apache.lucene.document Document Field FieldType Field$Store StringField)
           (org.apache.lucene.search.highlight Highlighter QueryScorer
                                               SimpleHTMLFormatter)
           (org.apache.lucene.index DirectoryReader IndexReader IndexOptions
                                    IndexWriter IndexWriterConfig Term)
           (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.store FSDirectory)
           (org.apache.lucene.search IndexSearcher BooleanQuery TermQuery
                                     BooleanClause$Occur BooleanQuery$Builder)
           (org.apache.lucene.util BytesRef)
           (java.nio.file Paths))
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private analyzer (StandardAnalyzer.))

(defn disk-index
  "Creates or opens a lucene index at the provided `file-path`."
  [file-path]
  (FSDirectory/open (Paths/get (.toURI file-path))))

(defn- entry->field
  [e]
  (let [field-name (name (key e))]
    (Field. field-name
          (str (val e))
          (doto (FieldType.)
            (.setStored true)
            (.setIndexOptions IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
            (.setTokenized (if-not (= "id" field-name) true false))))))

(defn- map->doc
  [m]
  (let [augmented-map (assoc m :id (:url m)) ;; add id field for de-duping to all docs
        fields (for [e augmented-map] (entry->field e))
        doc (Document.)]
    (doseq [f fields]
      (.add doc f))
    doc))

(defn add!
  "Adds documents to a lucene index.
  Will first delete any duplicate document(s) within the index before adding the
  new values. Uniqueness is determined by url.
  Excpects `maps` to be a coll of maps with keys denoting fields and values content."
  [index maps]
  (with-open [writer (IndexWriter. index (IndexWriterConfig. analyzer))]
    (let [docs (map #(hash-map :url (:url %) :doc (map->doc %)) maps)]
      (doseq [d docs]
        (.updateDocument writer (Term. "id" (:url d)) (:doc d))))))

(defn delete!
  "Deletes documents from a lucene index.
  N.B. as document urls are tokenized to support search, deleting by url requires
  keying by the `id` field, e.g:
  ```
  (delete! index [{:id http://foo.com }])
  ```
  Expects `maps` to be a coll of maps with keys denoting fields and values content."
  [index maps]
  (with-open [writer (IndexWriter. index (IndexWriterConfig. analyzer))]
    (doseq [m maps]
      (let [builder (BooleanQuery$Builder.)]
        (doseq [[k v] m]
          (.add builder (TermQuery. (Term. (name k) v)) BooleanClause$Occur/MUST))
        (.deleteDocuments writer (into-array (vector (.build builder))))))))

(defn- doc->map
  [highlighter doc]
  (into {} (map (fn [field]
                  (let [name (.name field)
                        value (.stringValue field)]
                    (if (= "content" name)
                      [name (str/join " ... " (seq (.getBestFragments highlighter analyzer name value 2)))]
                      [name value])))
                (.getFields doc))))

(defn search
  "Searches a lucene index against the provided `search-req`.
  Always uses lucene's default QueryParser for search."
  [index search-req]
   (with-open [reader (DirectoryReader/open index)]
    (let [searcher (IndexSearcher. reader)
          parser (QueryParser. (or (:field search-req) "content") analyzer)
          query (.parse parser (:query search-req))
          scorer (QueryScorer. query)
          highlighter (Highlighter. (SimpleHTMLFormatter. "<b>" "</b>") scorer)
          hits (.search searcher query Integer/MAX_VALUE)]
      (doall
       (for [hit (map (partial aget (.scoreDocs hits)) (range (.value (.totalHits hits))))]
         (->> (.doc hit)
              (.doc searcher)
              (doc->map highlighter)))))))


