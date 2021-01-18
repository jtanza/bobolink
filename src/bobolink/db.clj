(ns bobolink.db
  (:require [clojure.java.jdbc :as jdbc]))

(def db-spec {:dbtype "h2" :dbname "./db/bobodb"})

(defn create-user
  [user]
  (first (jdbc/insert! db-spec :user user)))

(comment (defn get-user
   [email]
   (jdbc/query db-spec ["select id, email, from user where email = ?" email])))

(defn get-user
  [creds]
  (let [query #(vector (str "select id, email from user where " %1 " = ?") %2)]
    (jdbc/query db-spec
                (cond
                  (contains? creds :email) (query "email" (:email creds))
                  (contains? creds :id) (query "id" (:id creds))))))

(defn get-user-full
  [email]
  (first (jdbc/query db-spec ["select id, email, password from user where email = ?" email])))

(defn set-auth-token
  [user token]
  (let [userid (:id user)]
    (jdbc/update! db-spec :token {:userid userid :token token} ["userid = ?" userid])))

(defn get-auth-token
  [userid]
  (->> ["select userid, authtoken from token where userid = ?" userid]
      (jdbc/query db-spec)
      (first)))

(defn add-bookmarks
  [req]
  (let [id (:userid req)
        rows (for [url (:urls req)]
               (vector id url))]
    (jdbc/insert-multi! db-spec "bookmark" ["userid", "url"] rows)))

(defn get-bookmarks
  [userid]
  (jdbc/query db-spec ["select url from bookmark where userid = ?", userid]))

