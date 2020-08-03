(ns seaquell.syntax
  (:require [clojure.spec.alpha :as s]))

(s/def ::id keyword?)

(s/def ::expr any?)

(s/def ::pos-num (s/and number? pos?))

(def <expr> :expr)
(def <ordering-item> :ordering-item)
(def <pos-num> :pos-num)
(def <bound> #{{:preceding <pos-num>}
               {:following <pos-num>}
               :current-row
               :unbounded-preceding
               :unbounded-following})
(def <id> :id)

(def <windef>
  {:base-win :kw-or-nil
   :partition-by [<expr>]
   :order-by [<ordering-item>]
   :bounds [<bound>]
   :exclude #{:no-others :group :current-row :ties}})

(def <id-or-windef (or <id> <windef>))

(def field
  {:field <id>
   :as <id>
   :filter-where <expr>
   :over <id-or-windef})

(defn windef? [w] (and (map? w) (contains? w :base-win)))

(comment
  (over :w1) => {:over :w1}
  (over :w1 (partition-by :a)) => {:base-win :w1, :partition-by [:a]}
  (over (partition-by :a)) => {:base-win nil, :partition-by [:a]}
  
  (win :w1 :as (windef ...)) => {:wins {:win :w1 :as <windef>}}
  (win :w1 (as (windef ...))) => {:wins {:win :w1 :as <windef>}}
  (win :w1 :as [windef args ...]) => {:wins {:win :w1 :as <windef>}}
  (win :w1 (as [windef args ...])) => {:wins {:win :w1 :as <windef>}}
  (win :w1 [windef args ...]) => {:wins {:win :w1 :as <windef>}}
  (win <windef> ...) => (mk-map* <windef> ...)

  ;; alter table
  (alter-table :schema.tbl ...)
  (alter-table :tbl ...)
  (alter-table :tbl (rename-to :new-tbl))
  (alter-table :tbl (rename (to :new-tbl)))
  (alter-table :tbl (rename-column :old-col :new-col))
  (alter-table :tbl (rename-column :old-col (to :new-col)))
  (alter-table :tbl (rename-column (column :old-col) (to :new-col)))
  (def rename rename-column)
  (alter-table :tbl (add-column <column-def>))
  (def add add-column)
  (s/def ::rename-column (s/keys :req-un [::column ::to]))
  (s/def ::alter-table (s/keys :req-un [::sql-stmt ::table] :opt-un [::rename-to ::rename-column ::add-column]))

  ;; analyze
  (analyze)
  (analyze :schema)
  (analyze :tbl-or-idx)
  (analyze :schema.tbl-or-idx)

  ;; attach/detach
  (attach :path-to-file (as :schema))
  (attach-database :path-to-file (as :schema))
  (detach :schema)
  (detach-database :schema)

  ;; create index
  (create-index ...)
  (create-index-if-not-exists ...)
  (create-unique-index ...)
  (create-unique-index-if-not-exists ...)
  (create-index (if-not-exists) :schema.idx ...)
  (create-index IF-NOT-EXISTS :schema.idx ...)
  (create-index :schema.idx (if-not-exists) ...)
  (create-index :schema.idx IF-NOT-EXISTS ...)
  (create-index :schema.idx (on :tbl) (columns :c1 :c2))
  (create-index :schema.idx (on :tbl) (columns (column :c1) :c2))
  (create-index :schema.idx (on :tbl) (columns (column :c1 (collate :nocase) ASC) :c2))
  (create-index :schema.idx (on :tbl) (columns [:c1 (collate :nocase) ASC] :c2))
  (create-index :schema.idx (on :tbl) [:c1 :c2] (where <expr>))

  ;; create table
  (create-table :schema.tbl ...)
  (create-table-if-not-exists :schema.tbl ...)
  (create-table :schema.tbl (if-not-exists) ...)
  (create-table :schema.tbl IF-NOT-EXISTS ...)
  (create-temp-table :schema.tbl ...)
  (create-temp-table-if-not-exists :schema.tbl ...)
  (create-temporary-table :schema.tbl ...)
  (create-temporary-table-if-not-exists :schema.tbl ...)
  (create-table :tbl (as (select ...)))
  (create-table :tbl (select ...)) ;hmm maybe
  (create-table :tbl [<column-defs> <tbl-constraints>?])
  (create-table :tbl
                (column-defs <col1> <col2> ...)
                (constraints ...)
                #_(without-rowid))
  (create-table :tbl [<column-defs> <tbl-constraints>?] (without-rowid))
  (create-table :tbl [<column-defs> <tbl-constraints>?] WITHOUT-ROWID)

  ;; virtual tables
  (create-virtual-table :vt (using :rtree <arg1> <arg2> ...))
  (create-virtual-table :vt (if-not-exists) (using :module-name))
  (create-virtual-table :vt (if-not-exists) (using :module-name <arg1> <arg2> ...))
  ;; maybe let each module arg use the raw sql dsl

  ;; column definitions
  (column-defs
    (column-def ...)
    (column-def
      :col-name
      ::type-name
      (constraints
        (constraint ...)
        (constraint :name (primary-key ...))
        (constraint :name (not-null #_(on-conflict ...)))
        (constraint :name (unique #_(on-conflict ...)))
        (constraint :name (check <expr>))
        (constraint :name (default <expr>))
        (constraint :name (collate :collation-name))
        (constraint :name (references ...))
        (constraint :name (generated-always-as <expr> #_(or (stored) (virtual))))
        (constraint :name (generated-always (as <expr>) #_(or (stored) (virtual))))
        (primary-key ...)
        (not-null #_(on-conflict ...))
        (unique #_(on-conflict ...))
        (check <expr>)
        (default <expr>)
        (collate :collation-name)
        (references ...)
        (generated-always-as <expr> #_(or (stored) (virtual)))
        (generated-always (as <expr>) #_(or (stored) (virtual)))))
    (column-def
      :col-name
      ::type-name
      (constraints
        []))
    [:col-name ::type-name ::col-constraints])

  (s/def :col/primary-key  (s/keys :opt-un [::order ::on-conflict ::autoincrement]))
  (s/def ::not-null (s/keys :opt-un [::on-conflict]))
  (s/def :col/unique (s/keys :opt-un [::on-conflict]))
  (s/def ::generated-always (s/keys :req-un [::as] :opt-un [::stored ::virtual]))
  (s/def ::col-constraint
    (s/or :pk :col/primary-key :nn ::not-null :uniq :col/unique :chk ::check :def ::default
          :coll ::collate :ref ::references))
  (s/def ::col-constraints (s/coll-of ::col-constraint))

  ;; table constraints
  (constraints ...)
  (constraints
    (constraint :name (primary-key ...))
    (constraint :name (unique ...))
    (constraint :name (check <expr>))
    (constraint :name (foreign-key [:c1 :c2] (references ...)))
    (primary-key ...)
    (unique ...)
    (check <expr>)
    (foreign-key [:c1 :c2] (references ...))
    )

  ;; tbl primary key or unique
  (primary-key ...)
  (unique ...)
  (primary-key
    (columns (column ...))
    (on-conflict ...))
  (primary-key [(column :c1 ...) (column :c2 ...)] #_(on-conflict))
  (primary-key [[:c1 ...] [:c2 ...]] #_(on-conflict))
  (primary-key [:c1 [:c2 ...]] #_(on-conflict))

  ;; on-delete (same for on-update)
  (on-delete :set-null)
  (on-delete :set-default)
  (on-delete :cascade)
  (on-delete :restrict)
  (on-delete :no-action)

  ;; deferrable
  (deferrable)
  (deferrable-initially-deferred)
  (deferrable-initially-immediate)
  (not-deferrable)
  (not-deferrable-initially-deferred)
  (not-deferrable-initially-immediate)
  (s/def ::initially #{:deferred :immediate nil})
  (s/def ::modifier #{:not 'not nil})
  (s/def ::deferrable (s/keys :opt-un [::initially ::modifier]))

  ;; on conflict
  (on-conflict-rollback)
  (on-conflict-abort)
  (on-conflict-fail)
  (on-conflict-ignore)
  (on-conflict-replace)
  (on-conflict :rollback)
  (on-conflict :abort)
  (on-conflict :fail)
  (on-conflict :ignore)
  (on-conflict :replace)
  (s/def :ddl/on-conflict #{:rollback :abort :fail :ignore :replace})

  ;; references
  (references
    :foreign-tbl
    #_[:c1 :c2]
    #_(on-delete :set-null)
    #_(on-update <action>)
    #_(match :name)
    (or (deferrable (initially :deferred))))
  (s/def ::references (s/keys :req-un [::table] :opt-un [::columns ::on-delete ::on-update ::match]))

  ;; foreign key clause
  (foreign-key [:c1 :c2] (references ...))

  ;; create trigger
  (create-trigger
    :mytrig
    ;;one of before, after, or instead-of
    (before :delete)
    #_(after :insert)
    #_(instead-of (update-of :col1 :col2))
    (on :tbl)
    (for-each-row)
    (when expr)
    (statements
      (insert ...)
      (select ...)
      (update ...)
      (delete ...))
    )
  (def IF-NOT-EXISTS (if-not-exists))
  (def FOR-EACH-ROW (for-each-row))
  (create-trigger (if-not-exists) :trig ...)
  (create-trigger IF-NOT-EXISTS :trig ...)
  (create-trigger :trig ... (if-not-exists) ...)
  (create-trigger :trig ... IF-NOT-EXISTS ...)
  (create-temp-trigger :trig ...)
  (create-temporary-trigger :trig ...)

  ;; create view
  (create-view ...)
  (create-view-if-not-exists ...)
  (create-view (if-not-exists) :schema.view ...)
  (create-view IF-NOT-EXISTS :schema.view ...)
  (create-view :schema.view (if-not-exists) ...)
  (create-view :schema.view IF-NOT-EXISTS ...)
  (create-view :view (columns :c1 :c2) (as (select ...)))
  (create-view :view [:c1 :c2] (select ...))

  ;; drop (parallel structure for dropping index, trigger, or view)
  ;syntax 1
  (drop* (if-exists) (table :t))
  (drop* (table :t) (if-exists))
  ;syntax 2
  (drop* :t (entity :table) (if-exists))
  (drop* :t (if-exists) (entity :table))

  (drop-table :schema.tbl)
  (drop-table :tbl)
  (drop-table (if-exists) :tbl)
  (drop-table IF-EXISTS :tbl)
  (drop-table-if-exists :tbl)
  (drop-table :tbl (if-exists))
  (drop-table :tbl IF-EXISTS)
  (s/def ::if-exists boolean?)
  (s/def ::drop-table (s/keys :req-un [::sql-stmt ::table ::if-exists]))

  ;; pragma
  (pragma :schema.pragma) ;gets the current value
  (pragma :pragma) ;gets the current value
  (pragma :schema.pragma <value>)
  (pragma :pragma <value>)  ;uses PRAGMA pragma (value);
  (pragma= :pragma <value>) ;uses PRAGMA pragma = value;
  (pragma <expr>) ;uses syntax of expression
  (pragma [= :pragma <value>]) => PRAGMA pragma = value;
  (pragma [:pragma <value>]) => PRAGMA pragma(value);
  {:sql-stmt :pragma, :pragma <pragma> :expr <value> :equals <bool>}

  ;; reindex
  (reindex)
  (reindex :collation-name)
  (reindex :schema.tbl-or-idx)
  (reindex :tbl-or-idx)

  ;; release/savepoint
  (savepoint :savepoint-name)
  (release :savepoint-name)
  (release-savepoint :savepoint-name)
  (rollback ...)

  ;; transactions
  (begin)
  (begin-transaction)
  (begin-deferred)
  (begin-deferred-transaction)
  (begin-immediate)
  (begin-immediate-transaction)
  (begin-exclusive)
  (begin-exclusive-transaction)
  (commit)
  (commit-transaction)
  (end)
  (end-transaction)
  (rollback)
  (rollback (to :savepoint-name))
  (rollback (to-savepoint :savepoint-name))
  (rollback-transaction)
  (rollback-transaction (to :savepoint-name))
  (rollback-transaction (to-savepoint :savepoint-name))
  (rollback-to :savepoint-name)
  (rollback-to-savepoint :savepoint-name)
  (rollback-transaction-to :savepoint-name)
  (rollback-transaction-to-savepooint :savepoint-name)

  ;; by default, jdbc/execute! wraps things in a transaction
  (sql! '[vacuum \;] (db c) (transaction? false))
  (vacuum (db c))
  (vacuum :main (db c)) ;same as above
  (vacuum :myschema (db c)) ;for an attached db
  (vacuum :into "file")
  (vacuum (sql/into "file"))
  (vacuum :schema (sql/into "file"))

  (insert-into
    :tbl (values [1 2 3])
    (on-conflict
      (columns
        (column :c1 (collate :col1) (order :asc))
        (column :c2 (collate :col2) (order :desc))
        :c3)
      (where {:c1 [> :c2]})
      (do-nothing)))

  (insert-into
    :tbl (values [1 2 3])
    (on-conflict
      [(column :c1 (collate :col1) (order :asc))
       (column :c2 (collate :col2) (order :desc))
       :c3]
      (where {:c1 [> :c2]})
      (do-nothing)))

  (insert-into
    :tbl (values [1 2 3])
    (on-conflict
      [[:c1 (collate :col1) (order :asc)]
       [:c2 (collate :col2) (order :desc)]
       :c3]
      (where {:c1 [> :c2]})
      (do-nothing)))

  (insert-into
    :tbl (values [1 2 3])
    (on-conflict
      ...
      (do-update
        (set-cols :c1 1 :c2 :excluded.c2)
        (where ...))))
) ; end comment

