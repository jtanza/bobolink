(defproject bobolink "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/java.jdbc "0.6.0"]
                 [clucy "0.4.0"]
                 [com.h2database/h2 "1.4.200"]
                 [compojure "1.6.1"]
                 [crypto-password "0.2.1"]
                 [org.jsoup/jsoup "1.7.3"]
                 [ring-basic-authentication "1.1.0"]
                 [ring/ring-core "1.8.2"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.0"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler bobolink.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
