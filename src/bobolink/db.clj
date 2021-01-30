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
                                 ["select id, email from bobouser where email = ?" email]
                                 ["select id, email from bobouser where id = ?" (Integer/parseInt id)])))))

(defn get-user-full
  [email]
  (first (jdbc/query db-spec ["select id, email, password from bobouser where email = ?" email])))

(defn set-auth-token
  [user token]
  (let [userid (:id user)]
    (jdbc/update! db-spec :token {:userid userid :authtoken token} ["userid = ?" userid])))

(defn set-auth-token
  [user token]
  (let [userid (:id user)]
    (jdbc/execute! db-spec ["insert into token values (?, ?) on conflict (userid) do update set authtoken = excluded.authtoken"
                            userid token])))

(defn get-auth-token
  [user]
  (when (seq user)
    (->> ["select userid, authtoken from token where userid = ?" (:id user)]
         (jdbc/query db-spec)
         (first))))

(defn add-bookmarks
  [user urls]
  (let [userid (:id user)]
    (doseq [url urls]
      (jdbc/execute! db-spec ["insert into bookmark values (?, ?) on conflict (userid, url) do nothing" userid url]))))

(defn get-bookmarks
  [user]
  (jdbc/query db-spec ["select url from bookmark where userid = ?" (:id user)]))

