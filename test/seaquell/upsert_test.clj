(ns seaquell.upsert-test
  "Uses an in-memory Sqlite database to demonstrate the use of
  SQLite UPSERT (ON CONFLICT clause in an INSERT statement)."
  (:refer-clojure :exclude [drop into update partition-by])
  (:require [clojure.java.jdbc :as jdb]
            [midje.sweet :refer :all]
            [diesel.edit :refer [edit-in]]
            [seaquell.core :refer :all]
            [seaquell.engine :refer :all]
            [seaquell.sqlite :refer [db-spec]]))

(let [c (db-conn (db-spec))
      q (insert
          :vocabulary [:word] (value "jovial")
          (on-conflict [:word]
                       (do-update (set-cols {:count [+ :count 1]})))
          (db c))]
  (do-sql "CREATE TABLE vocabulary(word TEXT PRIMARY KEY, count INT DEFAULT 1);" (db c))
  (fact "vocabulary is empty"
        (select-from! :vocabulary (db c)) => [])
  (insert! q)
  (fact "count for jovial is 1"
        (select-from! :vocabulary (db c)) => [{:word "jovial", :count 1}])
  (insert! q)
  (fact "count for jovial is 2"
        (select-from! :vocabulary (db c)) => [{:word "jovial", :count 2}]))

(let [c (db-conn (db-spec))
      q (insert
          :phonebook [:name :phonenumber] (value "Alice" :?)
          (on-conflict [:name]
                       (do-update (set-cols {:phonenumber
                                             :excluded.phonenumber})))
          (db c))]
  (do-sql "CREATE TABLE phonebook(name TEXT PRIMARY KEY, phonenumber TEXT);" (db c))
  (fact "phonebook is empty"
        (select-from! :phonebook (db c)) => [])
  (insert! q (params "407-555-1212"))
  (fact "insert adds initial record for Alice"
        (select-from! :phonebook (db c)) =>
        [{:name "Alice", :phonenumber "407-555-1212"}])
  (insert! q (params "704-555-1212"))
  (fact "upsert updates phone number for Alice"
        (select-from! :phonebook (db c)) =>
        [{:name "Alice", :phonenumber "704-555-1212"}]))

(let [c (db-conn (db-spec))
      q (insert
          :phonebook2 [:name :phonenumber :validDate] (value "Alice" :?1 :?2)
          (on-conflict [:name]
                       (do-update (set-cols {:phonenumber :excluded.phonenumber
                                             :validDate :excluded.validDate})
                                  (where {:excluded.validDate
                                          [> :phonebook2.validDate]})))
          (db c))]
  (do-sql "CREATE TABLE phonebook2
            (name TEXT PRIMARY KEY, phonenumber TEXT, validDate DATE);"
          (db c))
  (fact "phonebook2 is empty"
        (select-from! :phonebook2 (db c)) => [])
  (insert! q (params "704-555-1212" "2019-01-01"))
  (fact "insert adds initial record for Alice"
        (select-from! :phonebook2 (db c)) =>
        [{:name "Alice", :phonenumber "704-555-1212" :validdate "2019-01-01"}])
  (insert! q (params "704-555-1212" "2018-05-08"))
  (fact "upsert leaves prior phone number for Alice since it was newer"
        (select-from! :phonebook2 (db c)) =>
        [{:name "Alice", :phonenumber "704-555-1212" :validdate "2019-01-01"}]))

(let [c (db-conn (db-spec))
      q (insert
          :t1 (select :* (from :t2))
          (on-conflict [:x] (do-update (set-cols {:y :excluded.y})))
          (db c))]
  (do-sql "CREATE TABLE t1 (x PRIMARY KEY, y);" (db c))
  (do-sql "CREATE TABLE t2 (x PRIMARY KEY, y);" (db c))
  (insert! :t1 (values [1 4] [3 2]) (db c))
  (insert! :t2 (values [1 2] [3 4]) (db c))
  (fact "t1 has rows we intend to update"
        (select-from! :t1 (db c)) => [{:x 1 :y 4} {:x 3 :y 2}])
  (fact "upsert throws because ON could be for JOIN or ON CONFLICT"
        (insert! q) => (throws))
  (fact "adding a WHERE clause to SELECT resolves syntax ambiguity and upsert succeeds"
        (insert! (edit-in q [:values] (where true))) => [2]
        (select-from! :t1 (db c)) => [{:x 1 :y 2} {:x 3 :y 4}]))

