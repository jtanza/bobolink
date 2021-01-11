(ns bobolink.db
  (:require [clojure.java.jdbc :as jdbc]))

(def db-spec {:dbtype "h2" :dbname "./db/bobodb"})

(defn create-user
  [user]
  (jdbc/insert! db-spec :user user))

(defn get-user
  [email]
  (let [res (jdbc/query db-spec
                        ["select id, email from user where email = ?" email])]
    (assert (= (count res) 1))
    (first res)))

(defn set-auth-token
  [user token]
  (let [user-id (:id user)]
    (jdbc/update! db-spec :token {:userid user-id :token token} ["userid = ?" user-id])))

(defn get-auth-token
  [user]
  (let [res (jdbc/query db-spec
                        ["select userid, authtoken from token where userid = ?" (:id user)])]
    (first rest)))




