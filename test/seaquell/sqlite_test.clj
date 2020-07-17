(ns seaquell.sqlite-test
  "Tests functions in the `seaquell.sqlite` namespace"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all :exclude [schema]]
            [seaquell.engine :refer [db-conn executes?]]
            [seaquell.sqlite :refer :all]))

(fact "db-spec with no args returns a spec for an in-memory SQLite db"
  (db-spec) => {:classname "org.sqlite.JDBC"
                :subname ":memory:"
                :subprotocol "sqlite"})

(fact "db-spec with one arg returns a spec for a SQLite db file"
  (db-spec "C:/abs/path/to/my.db") => {:classname "org.sqlite.JDBC"
                                       :subname "C:/abs/path/to/my.db"
                                       :subprotocol "sqlite"}
  (db-spec "../rel/path/to/my.db") => {:classname "org.sqlite.JDBC"
                                       :subname "../rel/path/to/my.db"
                                       :subprotocol "sqlite"}
  (db-spec "./my.db") => {:classname "org.sqlite.JDBC"
                          :subname "./my.db"
                          :subprotocol "sqlite"}
  (db-spec "my.db") => {:classname "org.sqlite.JDBC"
                        :subname "my.db"
                        :subprotocol "sqlite"})

(fact "You can use `tables` and `schema` to browse the structure of a SQLite db"
  (let [c (db-conn (db-spec))]
    (fact
      "tables returns a sequence of table names"
      (tables c) => []
      (create-table! :t1 [:c1] (db c)) => executes?
      (tables c) => ["t1"]
      (create-table! :t2 [:c2 NOT-NULL] (db c)) => executes?
      (tables c) => ["t1" "t2"])
    (fact
      "schema returns the SQL string used to CREATE a table"
      (schema c :t1) => "CREATE TABLE t1 (c1)"
      (schema c :t2) => "CREATE TABLE t2 (c2 NOT NULL)")))

