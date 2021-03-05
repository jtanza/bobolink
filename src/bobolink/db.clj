(ns bobolink.db
  (:require [clojure.java.jdbc :as jdbc]
            [bobolink.util :as util]))

(def ^:private db {:dbtype "postgresql" :dbname "bobodb"
                   :user "bobouser" :password (:db-pw util/conf)})

(defn create-user
  [user]
  (first (jdbc/insert! db :bobouser user)))

(defn update-user
  [user]
  (jdbc/update! db :bobouser {:email (:email user) :password (:password user)} ["id = ?" (:id user)]))

(defn get-user
  "Returns the user represented by `creds` in the DB.
  `creds` should be a map containing either a user's email or id"
  [creds]
  (let [{:keys [email id]} creds]
    (first (jdbc/query db (if email
                                 ["select id, email from bobouser where email = ?" email]
                                 ["select id, email from bobouser where id = ?" (Integer/parseInt id)])))))

(defn get-user-full
  "Returns all columns associated with a user in the DB.
  Other functions returning users purposefully omit password data."
  [email]
  (first (jdbc/query db ["select id, email, password from bobouser where email = ?" email])))

(defn set-auth-token
  "Upserts an authentication token associated with a `user`."
  [user token]
  (let [{:keys [id]} user]
    (jdbc/execute! db ["insert into token values (?, ?) on conflict (userid) do update set authtoken = excluded.authtoken"
                            id token])))

(defn get-auth-token
  [user]
  (when (seq user)
    (->> ["select userid, authtoken from token where userid = ?" (:id user)]
         (jdbc/query db)
         (first))))

(defn destroy-auth-token
  [user]
  (when (seq user)
    (jdbc/delete! db :token ["userid = ?" (:id user)])))

(defn save-bookmarks
  [user urls]
  (let [{userid :id} user]
    (doseq [url urls]
      (jdbc/execute! db ["insert into bookmark values (?, ?) on conflict (userid, url) do nothing" userid url]))))

(defn delete-bookmarks
  [user urls]
  (let [{userid :id} user]
    (doseq [url urls]
      (jdbc/delete! db :bookmark ["userid = ? and url = ?" userid url]))))

(defn get-bookmarks
  [user]
  (jdbc/query db ["select url from bookmark where userid = ?" (:id user)]))

(defn get-reset-token
  "Returns a user's previously generated password reset token.
  Only returns non-expired tokens, regardless of whether or not a row may exist for the `user`."
  [user]
  (first (jdbc/query db ["select token from reset_token where userid = ? and expires > now()" (:id user)])))

(defn set-reset-token
  [user token]
  (jdbc/execute! db ["insert into reset_token values (?, ?, NOW() + INTERVAL '2 HOURS') on conflict (userid) do update set token = excluded.token, expires = excluded.expires"
                          (:id user) token]))

(defn destroy-reset-token
  [user]
  (when (seq user)
    (jdbc/delete! db :reset_token ["userid = ?" (:id user)])))

