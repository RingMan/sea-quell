(ns seaquell.create-test
  "Tests the SQLite CREATE command"
  (:refer-clojure
    :exclude [distinct drop group-by into set update partition-by when])
  (:require [diesel.core :refer [mk-map]]
            [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.engine :refer [db-conn executes? row-fn]]
            [seaquell.sqlite :refer [db-spec]]
            [seaquell.to-sql :refer [trigger-ev-to-sql]]))

(fact "Seaquell supports SQLite CREATE TABLE AS"
  (let [q (select :* (from :tt))
        as-q " AS SELECT * FROM tt;"]
    (fact "You can create a simple table."
      (create-table$ :t (as q)) => (str "CREATE TABLE t" as-q)
      (create-table$ :t :as q) => (str "CREATE TABLE t" as-q)
      (create$ (table :t) (as q)) => (str "CREATE TABLE t" as-q)
      (create$ (table :t) :as q) => (str "CREATE TABLE t" as-q))
    (fact "create-table is idempotent"
      (create-table (create-table :t (as q))) => (create-table :t (as q)))
    (fact "You can conditionally create a table"
      (create-table-if-not-exists$ :t (as q)) =>
      (str "CREATE TABLE IF NOT EXISTS t" as-q)
      (create-table$ :t (if-not-exists) (as q)) =>
      (str "CREATE TABLE IF NOT EXISTS t" as-q)
      (create-table$ :t IF-NOT-EXISTS (as q)) =>
      (str "CREATE TABLE IF NOT EXISTS t" as-q)
      (create-if-not-exists$ (table :t) (as q)) =>
      (str "CREATE TABLE IF NOT EXISTS t" as-q)
      (create$ (table :t) (if-not-exists) (as q)) =>
      (str "CREATE TABLE IF NOT EXISTS t" as-q)
      (create$ (table :t) IF-NOT-EXISTS (as q)) =>
      (str "CREATE TABLE IF NOT EXISTS t" as-q))
    (fact "You can create a temporary table"
      (create-temp-table$ :t (as q)) =>
      (str "CREATE TEMP TABLE t" as-q)
      (create-table$ :t (temp) (as q)) =>
      (str "CREATE TEMP TABLE t" as-q)
      (create-temp$ (table :t) (as q)) =>
      (str "CREATE TEMP TABLE t" as-q)
      (create$ (table :t) (temp) (as q)) =>
      (str "CREATE TEMP TABLE t" as-q)
      (create-temporary-table$ :t (as q)) =>
      (str "CREATE TEMPORARY TABLE t" as-q)
      (create-table$ :t (temporary) (as q)) =>
      (str "CREATE TEMPORARY TABLE t" as-q)
      (create-temporary$ (table :t) (as q)) =>
      (str "CREATE TEMPORARY TABLE t" as-q)
      (create$ (table :t) (temporary) (as q)) =>
      (str "CREATE TEMPORARY TABLE t" as-q))
    (fact "You can conditionally create a temporary table"
      (create-temp-table-if-not-exists$ :t (as q)) =>
      (str "CREATE TEMP TABLE IF NOT EXISTS t" as-q)
      (create-temp-table$ :t (if-not-exists) (as q)) =>
      (str "CREATE TEMP TABLE IF NOT EXISTS t" as-q)
      (create-temp-table$ :t IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMP TABLE IF NOT EXISTS t" as-q)
      (create-temp-if-not-exists$ (table :t) (as q)) =>
      (str "CREATE TEMP TABLE IF NOT EXISTS t" as-q)
      (create-temp$ (table :t) (if-not-exists) (as q)) =>
      (str "CREATE TEMP TABLE IF NOT EXISTS t" as-q)
      (create-temp$ (table :t) IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMP TABLE IF NOT EXISTS t" as-q)
      (create-temporary-if-not-exists$ (table :t) (as q)) =>
      (str "CREATE TEMPORARY TABLE IF NOT EXISTS t" as-q)
      (create-temporary$ (table :t) (if-not-exists) (as q)) =>
      (str "CREATE TEMPORARY TABLE IF NOT EXISTS t" as-q)
      (create-temporary$ (table :t) IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMPORARY TABLE IF NOT EXISTS t" as-q)
      (create-temporary-table-if-not-exists$ :t (as q)) =>
      (str "CREATE TEMPORARY TABLE IF NOT EXISTS t" as-q)
      (create-temporary-table$ :t (if-not-exists) (as q)) =>
      (str "CREATE TEMPORARY TABLE IF NOT EXISTS t" as-q)
      (create-temporary-table$ :t IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMPORARY TABLE IF NOT EXISTS t" as-q))))

(fact "You can specify column definitions to CREATE TABLE"
  (create-table$ :t [:a :b]) =>
  "CREATE TABLE t (a, b);"
  (create-table$ :t '[a b]) =>
  "CREATE TABLE t (a, b);"
  (create-table$ :t [(column :a) :b]) =>
  "CREATE TABLE t (a, b);"
  (create-table$ :t (columns :a :b)) =>
  "CREATE TABLE t (a, b);"
  (create-table$ :t (columns (column :a) (column :b))) =>
  "CREATE TABLE t (a, b);"
  (create-table$ :t ["a" :b]) => (throws #"Expected column name or definition"))

(fact "You can specify table constraints to CREATE TABLE"
  (fact "Table constraints may go at the end of a column definition vector"
    (create-table$ :t [:a :b (primary-key [:a :b])]) =>
    "CREATE TABLE t (a, b, PRIMARY KEY (a, b));"
    (create-table$ :t [:a :b (unique [:a :b])]) =>
    "CREATE TABLE t (a, b, UNIQUE (a, b));"
    (create-table$ :t [:a :b (unique [:a :b]) (check [> :a :b])
                       (foreign-key [:a] (references :tt [:x]))]) =>
    (str "CREATE TABLE t (a, b, UNIQUE (a, b), CHECK (a > b), "
         "FOREIGN KEY (a) REFERENCES tt (x));")
    )
  (fact "You can also specify table constraints using the `constraints` fn"
    (create-table$ :t [:a :b] (constraints (primary-key [:a :b]))) =>
    "CREATE TABLE t (a, b, PRIMARY KEY (a, b));"))

(fact "Since the CHECK constraint can apply to a column or the whole table,
      you may need to help seaquell know when to associate it with the
      last column"
  (fact "If the last constraint of the last column is a check constraint
        it will be treated as a table constraint"
    (create-table$
      :t [:x :y INTEGER (default 1) (check [> :y 0]) (unique [:x :y])]) =>
    "CREATE TABLE t (x, y INTEGER DEFAULT 1, CHECK (y > 0), UNIQUE (x, y));")
  (fact "By rearranging constraints, you can bind a CHECK constraint to the
        last column instead of the table"
    (create-table$
      :t [:x :y INTEGER (check [> :y 0]) (default 1) (unique [:x :y])]) =>
    "CREATE TABLE t (x, y INTEGER CHECK (y > 0) DEFAULT 1, UNIQUE (x, y));")
  (fact "You can also wrap a column definition in a vector to bind a CHECK
        constraint to the last column instead of the table"
    (create-table$
      :t [:x [:y INTEGER (default 1) (check [> :y 0])] (unique [:x :y])]) =>
    "CREATE TABLE t (x, y INTEGER DEFAULT 1 CHECK (y > 0), UNIQUE (x, y));")
  (fact "Finally, you can rearrange the column order so the CHECK constraint
        associates with the column without having to wrap it in a vector"
    (create-table$
      :t [:y INTEGER (default 1) (check [> :y 0]) :x (unique [:x :y])]) =>
    "CREATE TABLE t (y INTEGER DEFAULT 1 CHECK (y > 0), x, UNIQUE (x, y));"))

(fact "Seaquell supports SQLite CREATE VIEW"
  (let [q (select [:x :y] (from :t))
        as-q " AS SELECT x, y FROM t;"]
    (fact "You can create a simple view."
      (create-view$ :v (as q)) => (str "CREATE VIEW v" as-q)
      (create-view$ :v :as q) => (str "CREATE VIEW v" as-q)
      (create$ (view :v) (as q)) => (str "CREATE VIEW v" as-q)
      (create$ (view :v) :as q) => (str "CREATE VIEW v" as-q))
    (fact "create-view is idempotent"
      (create-view (create-view :v (as q))) => (create-view :v (as q)))
    (fact "You can conditionally create a view"
      (create-view-if-not-exists$ :v (as q)) =>
      (str "CREATE VIEW IF NOT EXISTS v" as-q)
      (create-view$ :v (if-not-exists) (as q)) =>
      (str "CREATE VIEW IF NOT EXISTS v" as-q)
      (create-view$ :v IF-NOT-EXISTS (as q)) =>
      (str "CREATE VIEW IF NOT EXISTS v" as-q)
      (create-if-not-exists$ (view :v) (as q)) =>
      (str "CREATE VIEW IF NOT EXISTS v" as-q)
      (create$ (view :v) (if-not-exists) (as q)) =>
      (str "CREATE VIEW IF NOT EXISTS v" as-q)
      (create$ (view :v) IF-NOT-EXISTS (as q)) =>
      (str "CREATE VIEW IF NOT EXISTS v" as-q))
    (fact "You can create a temporary view"
      (create-temp-view$ :v (as q)) =>
      (str "CREATE TEMP VIEW v" as-q)
      (create-view$ :v (temp) (as q)) =>
      (str "CREATE TEMP VIEW v" as-q)
      (create-temp$ (view :v) (as q)) =>
      (str "CREATE TEMP VIEW v" as-q)
      (create$ (view :v) (temp) (as q)) =>
      (str "CREATE TEMP VIEW v" as-q)
      (create-temporary-view$ :v (as q)) =>
      (str "CREATE TEMPORARY VIEW v" as-q)
      (create-view$ :v (temporary) (as q)) =>
      (str "CREATE TEMPORARY VIEW v" as-q)
      (create-temporary$ (view :v) (as q)) =>
      (str "CREATE TEMPORARY VIEW v" as-q)
      (create$ (view :v) (temporary) (as q)) =>
      (str "CREATE TEMPORARY VIEW v" as-q))
    (fact "You can conditionally create a temporary view"
      (create-temp-view-if-not-exists$ :v (as q)) =>
      (str "CREATE TEMP VIEW IF NOT EXISTS v" as-q)
      (create-temp-view$ :v (if-not-exists) (as q)) =>
      (str "CREATE TEMP VIEW IF NOT EXISTS v" as-q)
      (create-temp-view$ :v IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMP VIEW IF NOT EXISTS v" as-q)
      (create-temp-if-not-exists$ (view :v) (as q)) =>
      (str "CREATE TEMP VIEW IF NOT EXISTS v" as-q)
      (create-temp$ (view :v) (if-not-exists) (as q)) =>
      (str "CREATE TEMP VIEW IF NOT EXISTS v" as-q)
      (create-temp$ (view :v) IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMP VIEW IF NOT EXISTS v" as-q)
      (create$ (temp) (view :v) IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMP VIEW IF NOT EXISTS v" as-q)
      (create-temporary-if-not-exists$ (view :v) (as q)) =>
      (str "CREATE TEMPORARY VIEW IF NOT EXISTS v" as-q)
      (create-temporary$ (view :v) (if-not-exists) (as q)) =>
      (str "CREATE TEMPORARY VIEW IF NOT EXISTS v" as-q)
      (create-temporary$ (view :v) IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMPORARY VIEW IF NOT EXISTS v" as-q)
      (create-temporary-view-if-not-exists$ :v (as q)) =>
      (str "CREATE TEMPORARY VIEW IF NOT EXISTS v" as-q)
      (create-temporary-view$ :v (if-not-exists) (as q)) =>
      (str "CREATE TEMPORARY VIEW IF NOT EXISTS v" as-q)
      (create-temporary-view$ :v IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMPORARY VIEW IF NOT EXISTS v" as-q)
      (create$ (temporary) (view :v) IF-NOT-EXISTS (as q)) =>
      (str "CREATE TEMPORARY VIEW IF NOT EXISTS v" as-q))
    (fact "You can specify the column names of a view. `create-view` will
          accept a vector of column names immediately after the view name."
      (create-view$ :v [:a :b] (as q)) =>
      (str "CREATE VIEW v (a, b)" as-q)
      (create-view$ :v (columns :a :b) (as q)) =>
      (str "CREATE VIEW v (a, b)" as-q)
      (create$ (view :v) (columns :a :b) (as q)) =>
      (str "CREATE VIEW v (a, b)" as-q)
      (create-view$ :v (columns (column :a) (column :b)) (as q)) =>
      (str "CREATE VIEW v (a, b)" as-q)
      (create$ (view :v) (columns (column :a) (column :b)) (as q)) =>
      (str "CREATE VIEW v (a, b)" as-q))))

(fact "Seaquell supports SQLite CREATE INDEX"
  (let [on-t " ON t (c);"]
    (fact "You can create a simple index."
      (create-index$ :ix (on :t [:c])) => (str "CREATE INDEX ix" on-t)
      (create-index$ :ix (on :t (columns :c))) => (str "CREATE INDEX ix" on-t)
      (create$ (index :ix) (on :t [:c])) => (str "CREATE INDEX ix" on-t))
    (fact "create-index is idempotent"
      (create-index (create-index :ix (on :t [:c])))
      => (create-index :ix (on :t [:c])))
    (fact "You can conditionally create a index"
      (create-index-if-not-exists$ :ix (on :t [:c])) =>
      (str "CREATE INDEX IF NOT EXISTS ix" on-t)
      (create-index$ :ix (if-not-exists) (on :t [:c])) =>
      (str "CREATE INDEX IF NOT EXISTS ix" on-t)
      (create-index$ :ix IF-NOT-EXISTS (on :t [:c])) =>
      (str "CREATE INDEX IF NOT EXISTS ix" on-t)
      (create-if-not-exists$ (index :ix) (on :t [:c])) =>
      (str "CREATE INDEX IF NOT EXISTS ix" on-t)
      (create$ (index :ix) (if-not-exists) (on :t [:c])) =>
      (str "CREATE INDEX IF NOT EXISTS ix" on-t)
      (create$ (index :ix) IF-NOT-EXISTS (on :t [:c])) =>
      (str "CREATE INDEX IF NOT EXISTS ix" on-t))
    (fact "You can create a unique index"
      (create-unique-index$ :ix (on :t [:c])) =>
      (str "CREATE UNIQUE INDEX ix" on-t)
      (create-index$ :ix (unique) (on :t [:c])) =>
      (str "CREATE UNIQUE INDEX ix" on-t)
      (create$ (index :ix) (unique) (on :t [:c])) =>
      (str "CREATE UNIQUE INDEX ix" on-t))
    (fact "You can conditionally create a unique index"
      (create-unique-index-if-not-exists$ :ix (on :t [:c])) =>
      (str "CREATE UNIQUE INDEX IF NOT EXISTS ix" on-t)
      (create-index-if-not-exists$ :ix (unique) (on :t [:c])) =>
      (str "CREATE UNIQUE INDEX IF NOT EXISTS ix" on-t)
      (create-unique-index$ :ix (if-not-exists) (on :t [:c])) =>
      (str "CREATE UNIQUE INDEX IF NOT EXISTS ix" on-t)
      (create-unique-index$ :ix IF-NOT-EXISTS (on :t [:c])) =>
      (str "CREATE UNIQUE INDEX IF NOT EXISTS ix" on-t)
      (create$ (unique) (index :ix) (if-not-exists) (on :t [:c])) =>
      (str "CREATE UNIQUE INDEX IF NOT EXISTS ix" on-t))
    (fact "You can specify the collation name and sort order of an indexed column"
      (create-index$ :idx (on :t [[:c (collate :rtrim) (asc)]])) =>
      "CREATE INDEX idx ON t (c COLLATE rtrim ASC);"
      (create-index$ :ix2 (on :t [[:c (collate :nocase) (desc)]])) =>
      "CREATE INDEX ix2 ON t (c COLLATE nocase DESC);")
    (fact "You can specify a WHERE clause when creating an index"
      (create-index$ :ix (on :t [:c]) (where {:c [:is-not nil]})) =>
      "CREATE INDEX ix ON t (c) WHERE c IS NOT NULL;")))

(fact "CREATE VIRTUAL TABLE works!"
  (create$ (virtual-table :vt) (using :rtree :x :y :z)) =>
  "CREATE VIRTUAL TABLE vt USING rtree(x, y, z);"
  (create-virtual-table (create-virtual-table :vt (using :rtree :x :y :z)))
  => (create-virtual-table :vt (using :rtree :x :y :z))
  (let [c (db-conn (db-spec))
        q (select :* (from :vt))]
    (create-virtual-table! :vt (using :rtree :x :y :z) (db c)) => executes?
    (create-virtual-table! :vt (using :rtree :x :y :z) (db c))
    => (throws #"table vt already exists")
    (create-virtual-table-if-not-exists!
      :vt (using :rtree :x :y :z) (db c)) => executes?
    (insert-into! :vt (values [1 2 3] [4 5 6] [7 8 9]) (db c)) => [3]
    (select! [[count :*] (as :n)] (from :vt) (db c) (row-fn :n)) => [3]))

(fact "Seaquell supports SQLite CREATE TRIGGER"
  (let [->sql (fn [& xs] (-> xs mk-map trigger-ev-to-sql))]
    (fact "You can say what database operation fires a trigger."
      (->sql (delete)) => "DELETE"
      (->sql (op :delete)) => "DELETE"
      (->sql (insert)) => "INSERT"
      (->sql (op :insert)) => "INSERT"
      (->sql (update-of :c1 :c2)) => "UPDATE OF c1, c2")
    (fact "You can fire a trigger before deleting rows."
      (->sql (before-delete)) => "BEFORE DELETE"
      (->sql (before :delete)) => "BEFORE DELETE"
      (->sql (before (delete))) => "BEFORE DELETE"
      (->sql (before) (delete)) => "BEFORE DELETE"
      (->sql (fire :before :delete)) => "BEFORE DELETE"
      (->sql (fire :before (delete))) => "BEFORE DELETE"
      (->sql (fire :before) (delete)) => "BEFORE DELETE")
    (fact "You can fire a trigger before inserting rows."
      (->sql (before-insert)) => "BEFORE INSERT"
      (->sql (before :insert)) => "BEFORE INSERT"
      (->sql (before (insert))) => "BEFORE INSERT"
      (->sql (before) (insert)) => "BEFORE INSERT"
      (->sql (fire :before :insert)) => "BEFORE INSERT"
      (->sql (fire :before (insert))) => "BEFORE INSERT"
      (->sql (fire :before) (insert)) => "BEFORE INSERT")
    (fact "You can fire a trigger before updating rows"
      (->sql (before-update-of :c)) => "BEFORE UPDATE OF c"
      (->sql (before (update-of :c))) => "BEFORE UPDATE OF c"
      (->sql (before) (update-of :c)) => "BEFORE UPDATE OF c"
      (->sql (fire :before (update-of :c))) => "BEFORE UPDATE OF c"
      (->sql (fire :before) (update-of :c)) => "BEFORE UPDATE OF c")
    (fact "You can fire a trigger after deleting rows"
      (->sql (after-delete)) => "AFTER DELETE"
      (->sql (after :delete)) => "AFTER DELETE"
      (->sql (after (delete))) => "AFTER DELETE"
      (->sql (after) (delete)) => "AFTER DELETE"
      (->sql (fire :after :delete)) => "AFTER DELETE"
      (->sql (fire :after (delete))) => "AFTER DELETE"
      (->sql (fire :after) (delete)) => "AFTER DELETE")
    (fact "You can fire a trigger after inserting rows"
      (->sql (after-insert)) => "AFTER INSERT"
      (->sql (after :insert)) => "AFTER INSERT"
      (->sql (after (insert))) => "AFTER INSERT"
      (->sql (after) (insert)) => "AFTER INSERT"
      (->sql (fire :after :insert)) => "AFTER INSERT"
      (->sql (fire :after (insert))) => "AFTER INSERT"
      (->sql (fire :after) (insert)) => "AFTER INSERT")
    (fact "You can fire a trigger after updating rows"
      (->sql (after-update-of :c)) => "AFTER UPDATE OF c"
      (->sql (after (update-of :c))) => "AFTER UPDATE OF c"
      (->sql (after) (update-of :c)) => "AFTER UPDATE OF c"
      (->sql (fire :after (update-of :c))) => "AFTER UPDATE OF c"
      (->sql (fire :after) (update-of :c)) => "AFTER UPDATE OF c")
    (fact "You can fire a trigger instead of deleting rows"
      (->sql (instead-of-delete)) => "INSTEAD OF DELETE"
      (->sql (instead-of :delete)) => "INSTEAD OF DELETE"
      (->sql (instead-of (delete))) => "INSTEAD OF DELETE"
      (->sql (instead-of) (delete)) => "INSTEAD OF DELETE"
      (->sql (fire :instead-of :delete)) => "INSTEAD OF DELETE"
      (->sql (fire :instead-of (delete))) => "INSTEAD OF DELETE"
      (->sql (fire :instead-of) (delete)) => "INSTEAD OF DELETE")
    (fact "You can fire a trigger instead of inserting rows"
      (->sql (instead-of-insert)) => "INSTEAD OF INSERT"
      (->sql (instead-of :insert)) => "INSTEAD OF INSERT"
      (->sql (instead-of (insert))) => "INSTEAD OF INSERT"
      (->sql (instead-of) (insert)) => "INSTEAD OF INSERT"
      (->sql (fire :instead-of :insert)) => "INSTEAD OF INSERT"
      (->sql (fire :instead-of (insert))) => "INSTEAD OF INSERT"
      (->sql (fire :instead-of) (insert)) => "INSTEAD OF INSERT")
    (fact "You can fire a trigger instead of updating rows"
      (->sql (instead-of-update-of :c)) => "INSTEAD OF UPDATE OF c"
      (->sql (instead-of (update-of :c))) => "INSTEAD OF UPDATE OF c"
      (->sql (instead-of) (update-of :c)) => "INSTEAD OF UPDATE OF c"
      (->sql (fire :instead-of (update-of :c))) => "INSTEAD OF UPDATE OF c"
      (->sql (fire :instead-of) (update-of :c)) => "INSTEAD OF UPDATE OF c"))
  (let [event [(after-insert) (on :t)]
        event-s " AFTER INSERT ON t"
        action (begin (update :t (set {:x [:upper :x]}) (where {:t.id :new.id})))
        action-s " BEGIN UPDATE t SET x=UPPER(x) WHERE t.id = new.id; END;"
        sql-str (str event-s action-s)]
    (fact "You can create a basic trigger"
      (create-trigger$ :tt event action) => (str "CREATE TRIGGER tt" sql-str))
    (fact "create-trigger is idempotent"
      (create-trigger (create-trigger :tt event action))
      => (create-trigger :tt event action))
    (fact "You can add a WHEN clause to fire a TRIGGER conditionally"
      (create-trigger$ :tt event (when true) action) =>
      (str "CREATE TRIGGER tt" event-s " WHEN TRUE" action-s))
    (fact "You can specify FOR EACH ROW on a TRIGGER"
      (create-trigger$ :tt event (for-each-row) action) =>
      (str "CREATE TRIGGER tt" event-s " FOR EACH ROW" action-s))
    (fact "You can create a temporary TRIGGER"
      (create-temp-trigger$ :tt event action) =>
      (str "CREATE TEMP TRIGGER tt" event-s action-s)
      (create-trigger$ :tt (temp) event action) =>
      (str "CREATE TEMP TRIGGER tt" event-s action-s)
      (create-temp$ (trigger :tt) event action) =>
      (str "CREATE TEMP TRIGGER tt" event-s action-s)
      (create$ (temp) (trigger :tt) event action) =>
      (str "CREATE TEMP TRIGGER tt" event-s action-s)
      (create-temporary-trigger$ :tt event action) =>
      (str "CREATE TEMPORARY TRIGGER tt" event-s action-s)
      (create-trigger$ :tt (temporary) event action) =>
      (str "CREATE TEMPORARY TRIGGER tt" event-s action-s)
      (create-temporary$ (trigger :tt) event action) =>
      (str "CREATE TEMPORARY TRIGGER tt" event-s action-s)
      (create$ (temporary) (trigger :tt) event action) =>
      (str "CREATE TEMPORARY TRIGGER tt" event-s action-s))
    (fact "You can conditionally create a TRIGGER"
      (create-trigger-if-not-exists$ :tt event action) =>
      (str "CREATE TRIGGER IF NOT EXISTS tt" event-s action-s)
      (create-trigger$ :tt (if-not-exists) event action) =>
      (str "CREATE TRIGGER IF NOT EXISTS tt" event-s action-s)
      (create-if-not-exists$ (trigger :tt) event action) =>
      (str "CREATE TRIGGER IF NOT EXISTS tt" event-s action-s)
      (create$ (trigger :tt) (if-not-exists) event action) =>
      (str "CREATE TRIGGER IF NOT EXISTS tt" event-s action-s))
    (fact "You can conditionally create a temporary TRIGGER"
      (create-temp-trigger-if-not-exists$ :tt event action) =>
      (str "CREATE TEMP TRIGGER IF NOT EXISTS tt" event-s action-s)
      (create-temporary-trigger-if-not-exists$ :tt event action) =>
      (str "CREATE TEMPORARY TRIGGER IF NOT EXISTS tt" event-s action-s))))

(fact "Here's an end-to-end example of a trigger"
  (let [c (db-conn (db-spec))]
    (create-table! :t [:id INTEGER (primary-key AUTOINCREMENT) :x] (db c))
    => executes?
    (create-trigger!
      :trg (after-insert) (on :t)
      (begin (update :t (set {:x [:upper :x]}) (where {:t.id :new.id}))) (db c))
    => executes?
    (insert-into! :t [:x] (values ["hi"] ["SeaQuell"] ["LiTe"]) (db c))
    => executes?
    (select-from! :t (row-fn :x) (db c)) => ["HI" "SEAQUELL" "LITE"]))

(fact "The RAISE function is available in TRIGGER programs"
  (let [c (db-conn (db-spec))]
    (create-table! :t [:x] (db c)) => executes?
    (create-trigger!
      :trg (before-insert) (on :t)
      (begin (select [`(case new.x "hi" (raise abort "\"hi\" is invalid!"))]))
      (db c))
    => executes?
    (insert-into! :t [:x] (value "hi") (db c)) => (throws #"\"hi\" is invalid!")
    (insert-into! :t [:x] (value "bye") (db c)) => executes?
    (select-from! :t (row-fn :x) (db c)) => ["bye"]))

