(require '[clojure.java.jdbc :as jdbc])

(jdbc/with-db-connection [conn {:dbtype "h2" :dbname "./db/bobodb"}]
  (jdbc/db-do-commands conn
                       ["create table user(id bigint primary key auto_increment, email varchar unique, password varchar)"
                        "create table token (userid bigint, authtoken varchar, foreign key(userid) references user(id))"]))

