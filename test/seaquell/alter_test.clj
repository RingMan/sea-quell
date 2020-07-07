(ns seaquell.alter-test
  "Uses an in-memory Sqlite database to demonstrate the use of
  SQLite ALTER TABLE commands."
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]))

(fact "Seaquell supports SQLite ALTER TABLE"
  (fact "alter and alter-table are idempotent"
    (alter (alter (table :t) (rename-to :tbl))) => (alter (table :t) (rename-to :tbl))
    (alter-table (alter-table :t (rename-to :tbl))) => (alter-table :t (rename-to :tbl)))
  (fact "You can rename a table"
    (let [alter-s "ALTER TABLE t RENAME TO tbl;"]
      (alter-table$ :t (rename :tbl)) => alter-s
      (alter-table$ :t (rename (to :tbl))) => alter-s
      (alter-table$ :t (rename-to :tbl)) => alter-s
      (alter$ (table :t) (rename :tbl)) => alter-s
      (alter$ (table :t) (rename (to :tbl))) => alter-s
      (alter$ (table :t) (rename-to :tbl)) => alter-s))
  (fact "You can rename a column"
    (let [alter-s "ALTER TABLE t RENAME c1 TO c2;"
          alter-col-s "ALTER TABLE t RENAME COLUMN c1 TO c2;"]
      (alter-table$ :t (rename :c1 :c2)) => alter-s
      (alter-table$ :t (rename :c1 (to :c2))) => alter-s
      (alter-table$ :t (rename (column :c1) :c2)) => alter-s
      (alter-table$ :t (rename (column :c1) (to :c2))) => alter-s
      (alter$ (table :t) (rename :c1 :c2)) => alter-s
      (alter$ (table :t) (rename :c1 (to :c2))) => alter-s
      (alter$ (table :t) (rename (column :c1) :c2)) => alter-s
      (alter$ (table :t) (rename (column :c1) (to :c2))) => alter-s
      (alter-table$ :t (rename-column :c1 :c2)) => alter-col-s
      (alter-table$ :t (rename-column :c1 (to :c2))) => alter-col-s
      (alter-table$ :t (rename-column (column :c1) :c2)) => alter-col-s
      (alter-table$ :t (rename-column (column :c1) (to :c2))) => alter-col-s
      (alter$ (table :t) (rename-column :c1 :c2)) => alter-col-s
      (alter$ (table :t) (rename-column :c1 (to :c2))) => alter-col-s
      (alter$ (table :t) (rename-column (column :c1) :c2)) => alter-col-s
      (alter$ (table :t) (rename-column (column :c1) (to :c2))) => alter-col-s))
  (fact "You can add a column. Give its name then its type (optional) and
        then any constraints (optional)."
    (let [alter-t "ALTER TABLE t ADD "
          col "COLUMN "
          c2 "c2;"
          c2-int "c2 INTEGER;"
          c2-42 "c2 DEFAULT 42;"
          c2-int-42 "c2 INTEGER DEFAULT 42;"]
      (alter-table$ :t (add :c2)) => (str alter-t c2)
      (alter$ (table :t) (add :c2)) => (str alter-t c2)
      (alter-table$ :t (add-column :c2)) => (str alter-t col c2)
      (alter$ (table :t) (add-column :c2)) => (str alter-t col c2)
      (alter-table$ :t (add :c2 INTEGER)) => (str alter-t c2-int)
      (alter$ (table :t) (add :c2 INTEGER)) => (str alter-t c2-int)
      (alter-table$ :t (add-column :c2 INTEGER)) => (str alter-t col c2-int)
      (alter$ (table :t) (add-column :c2 INTEGER)) => (str alter-t col c2-int)
      (alter-table$ :t (add :c2 (default 42))) => (str alter-t c2-42)
      (alter$ (table :t) (add :c2 (default 42))) => (str alter-t c2-42)
      (alter-table$ :t (add-column :c2 (default 42)))
      => (str alter-t col c2-42)
      (alter$ (table :t) (add-column :c2 (default 42)))
      => (str alter-t col c2-42)
      (alter-table$ :t (add :c2 INTEGER (default 42)))
      => (str alter-t c2-int-42)
      (alter$ (table :t) (add :c2 INTEGER (default 42)))
      => (str alter-t c2-int-42)
      (alter-table$ :t (add-column :c2 INTEGER (default 42)))
      => (str alter-t col c2-int-42)
      (alter$ (table :t) (add-column :c2 INTEGER (default 42)))
      => (str alter-t col c2-int-42))))

