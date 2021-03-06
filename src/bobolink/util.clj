(ns bobolink.util
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [taoensso.timbre :as timbre
             :refer [debug]]))

(defn load-edn
  "Reads in an `.edn` file located on disk at `source`"
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))
    (catch Exception e
      (debug e))))

(def conf (load-edn "conf.edn"))
