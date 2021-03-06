(defproject bobolink "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.cache "1.0.207"]
                 [org.clojure/java.jdbc "0.6.0"]
                 [amazonica "0.3.153"]
                 [clucie "0.4.2"]
                 [compojure "1.6.1"]
                 [crypto-password "0.2.1"]
                 [com.taoensso/timbre "5.1.2"]
                 [com.draines/postal "2.0.4"]
                 [org.apache.lucene/lucene-core "8.8.0"]
                 [org.apache.lucene/lucene-highlighter "8.8.0"]
                 [org.jsoup/jsoup "1.7.3"]
                 [org.postgresql/postgresql "42.1.4"]
                 [ring-basic-authentication "1.1.0"]
                 [ring-logger "1.0.1"]
                 [ring/ring-core "1.8.2"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.0"]
                 [ring-ratelimit "0.2.2"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler bobolink.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
