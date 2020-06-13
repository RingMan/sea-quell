(ns seaquell.reindex-test
  "Tests the SQLite REINDEX command"
  (:refer-clojure :exclude [into update partition-by])
  (:require [midje.sweet :refer :all]
            [seaquell.core :refer :all]
            [seaquell.engine :refer [db-conn executes?]]
            [seaquell.sqlite :refer [db-spec]]))

(fact "Seaquell supports the SQLite REINDEX statement"
  (reindex$) => "REINDEX;"
  (reindex$ :nocase) => "REINDEX nocase;" ;collation name
  (reindex$ :schema.tbl_or_idx) => "REINDEX schema.tbl_or_idx;"
  (reindex$ :tbl_or_idx) => "REINDEX tbl_or_idx;")

(let [s :myschema
      schema-tbl (keyword (str (name s) \. "tbl"))
      q (reindex schema-tbl)
      c (db-conn (db-spec))]
  (fact "REINDEX everything works!"
        (reindex! (db c)) => executes?)
  (fact "REINDEX collation name works!"
        (reindex! :nocase (db c)) => executes?)
  (fact "reindex is idempotent"
        (reindex q) => q)
  (fact "REINDEX indexes associated with a table in an attached schema"
        (attach! ":memory:" (as s) (db c)) => executes?
        (sql! `[create table ~schema-tbl (x) \;] (db c)) => executes?
        (reindex! schema-tbl (db c)) => executes?))

