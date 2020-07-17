(ns seaquell.values-test
  "Uses an in-memory Sqlite database to demonstrate the use of
  SQLite VALUES as a top-level statement."
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.engine :refer [db-conn]]
            [seaquell.sqlite :refer [db-spec]]))

(fact "You can use `value` as a top-level statement. Use :-- to separate
      the last column value from execution options"
  (let [c (db-conn (db-spec))]
    (value$ 1 2 nil "hi" :-- (db c)) => "VALUES (1, 2, NULL, 'hi');"
    (value! 1 2 nil "hi" :-- (db c))
    => [{:column1 1 :column2 2 :column3 nil :column4 "hi"}]
    (value! 1 2 nil :? :-- (db c) (params "hi"))
    => [{:column1 1 :column2 2 :column3 nil :column4 "hi"}]
    (sql! (value 1 2 nil :?) (db c) (params "hi"))
    => [{:column1 1 :column2 2 :column3 nil :column4 "hi"}])
  (fact "value is idempotent"
    (value (value 1 2 nil "hi")) => (value 1 2 nil "hi")))

(fact "You can use `values` as a top-level statement. Arguments after the
      last vector are execution options"
  (let [c (db-conn (db-spec))]
    (values$ [1 2] [nil "hi"] (db c)) => "VALUES (1, 2), (NULL, 'hi');"
    (values! [1 2] [nil "hi"] (db c))
    => [{:column1 1 :column2 2} {:column1 nil :column2 "hi"}]
    (sql! (values [1 2] [nil "hi"]) (db c))
    => [{:column1 1 :column2 2} {:column1 nil :column2 "hi"}])
  (fact "values is idempotent"
    (values (values [1 2] [nil "hi"])) => (values [1 2] [nil "hi"])))

