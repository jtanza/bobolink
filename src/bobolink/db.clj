(ns bobolink.db
  (:require [clojure.java.jdbc :as jdbc]))

(def db-spec {:dbtype "postgresql" :dbname "bobodb" :user "bobouser"})

(defn create-user
  [user]
  (first (jdbc/insert! db-spec :bobouser user)))

(defn get-user
  [creds]
  (let [{:keys [email id]} creds]
    (first (jdbc/query db-spec (if email
                                 ["select id, email from bobouser where email = ?", email]
                                 ["select id, email from bobouser where id = ?", id])))))

(defn get-user-full
  [email]
  (first (jdbc/query db-spec ["select id, email, password from bobouser where email = ?" email])))

(defn set-auth-token
  [user token]
  (let [userid (:id user)]
    (jdbc/update! db-spec :token {:userid userid :authtoken token} ["userid = ?" userid])))

(defn get-auth-token
  [user]
  (when (seq user)
    (->> ["select userid, authtoken from token where userid = ?" (:id user)]
         (jdbc/query db-spec)
         (first))))

;; TODO this should take a `user`
(defn add-bookmarks
  [req]
  (let [id (:userid req)
        rows (for [url (:urls req)]
               (vector id url))]
    (jdbc/insert-multi! db-spec "bookmark" ["userid", "url"] rows)))

(defn get-bookmarks
  [userid]
  (jdbc/query db-spec ["select url from bookmark where userid = ?", userid]))
