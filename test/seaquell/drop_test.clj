(ns seaquell.drop-test
  "Tests the SQLite DROP command"
  (:refer-clojure :exclude [distinct drop group-by into update partition-by])
  (:require [midje.sweet :refer :all]
            [seaquell.core :refer :all]
            [seaquell.engine :refer [db-conn executes?]]
            [seaquell.sqlite :refer [db-spec]]))

(fact "Seaquell supports the SQLite DROP TABLE statement"
  (drop-table$ :main.t) => "DROP TABLE main.t;"
  (drop-table$ :t) => "DROP TABLE t;"
  (drop-table$ :t (if-exists)) => "DROP TABLE IF EXISTS t;"
  (drop-table$ :t IF-EXISTS) => "DROP TABLE IF EXISTS t;"
  (drop-table-if-exists$ :t) => "DROP TABLE IF EXISTS t;")

(fact "Seaquell supports the SQLite DROP INDEX statement"
  (drop-index$ :ix) => "DROP INDEX ix;"
  (drop-index$ :ix (if-exists)) => "DROP INDEX IF EXISTS ix;"
  (drop-index-if-exists$ :ix) => "DROP INDEX IF EXISTS ix;")

(fact "Seaquell supports the SQLite DROP TRIGGER statement"
  (drop-trigger$ :trg) => "DROP TRIGGER trg;"
  (drop-trigger$ :trg (if-exists)) => "DROP TRIGGER IF EXISTS trg;"
  (drop-trigger-if-exists$ :trg) => "DROP TRIGGER IF EXISTS trg;")

(fact "Seaquell supports the SQLite DROP VIEW statement"
  (drop-view$ :v) => "DROP VIEW v;"
  (drop-view$ :v (if-exists)) => "DROP VIEW IF EXISTS v;"
  (drop-view-if-exists$ :v) => "DROP VIEW IF EXISTS v;")

(fact "The seaquell.core/drop fn works similarly"
  (drop$ (table :t)) => "DROP TABLE t;"
  (drop$ (table :t) (if-exists)) => "DROP TABLE IF EXISTS t;"
  (drop$ (if-exists) (table :t)) => "DROP TABLE IF EXISTS t;"
  (drop$ IF-EXISTS (table :t)) => "DROP TABLE IF EXISTS t;"
  (drop-if-exists$ (table :t)) => "DROP TABLE IF EXISTS t;"

  (drop$ (index :i)) => "DROP INDEX i;"
  (drop$ (trigger :t)) => "DROP TRIGGER t;"
  (drop$ (view :v)) => "DROP VIEW v;")

(fact "drop functions are idempotent"
  (drop (drop (table :t))) => (drop (table :t))
  (drop-table (drop-table :t)) => (drop-table :t)
  (drop-index (drop-index :ix)) => (drop-index :ix)
  (drop-trigger (drop-trigger :trg)) => (drop-trigger :trg)
  (drop-view (drop-view :v)) => (drop-view :v))

(fact "DROP TABLE works!"
  (let [c (db-conn (db-spec))]
    (sql! '[create table t(x) \;] (db c)) => executes?
    (drop-table! :t (db c)) => executes?
    (drop-table! :t (db c)) => (throws #"no such table: t")
    (drop-table-if-exists! :t (db c)) => executes?))

