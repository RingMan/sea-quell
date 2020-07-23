(ns seaquell.core
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [clojure.core :as c]
            [clojure.spec.alpha :as s]
            [diesel.core :refer :all]
            [diesel.edit :refer [conj-in]]
            [seaquell.engine :as eng]
            [seaquell.raw :as r]
            [seaquell.to-sql :as sql]
            [seaquell.util :refer :all]))

(def-props
  as binary check database default entity expr filter-where having initially
  index indexed-by into match modifier offset on-delete on-update op schema
  statement table to trigger view where)

(def-bool-props if-exists)
(def-map-props set)
(def-vec-props partition-by)

(defn raw
  ([x] {:raw x})
  ([x & xx] {:raw (->> xx (cons x) vec)}))

(defn sql [s & body]
  "Constructs a SQL statement or fragment from its arguments.
  If the first arg can act as a top-level statement, the body is composed
  into `stmt` using mk-map*. Otherwise, the leading arguments describe raw
  SQL (with leading strings treated as verbatim SQL text), while the trailing
  arguments are options to mix into the SQL value."
  (let [[stmt body]
        (cond
          (or (sql-stmt? s) (:values s)) [s body]
          :else (let [[strings body] (split-with string? (cons s body))
                      strings (map raw strings)
                      [tokens body] (split-with #(s/valid? ::r/sql-elem %)
                                                (concat strings body))]
                  [{:sql-stmt :sql, :tokens tokens}, body]))]
    (mk-map* stmt body)))

(defn field [f & more]
  (if (field? f)
    (mk-map* f more)
    (mk-map* {:field f} more)))

(defn fields* [acc [f aka & rem-fs :as fs]]
  (cond
    (empty? fs) acc
    (alias? aka) (recur (conj acc (field f aka)) rem-fs)
    (as? aka) (recur (conj acc (field f :as (first rem-fs))) (next rem-fs))
    :else (recur (conj acc f) (next fs))))

(defn fields [& fs]
  {:fields (fields* [] fs)})

(defn interval [ival units] {:interval ival :units units})

(defn cte [t & [cols & rem-body :as body]]
  (cond
    (:cte t) (mk-map* t body)
    (vector? cols) (mk-map* {:cte t} (cons {:columns cols} rem-body))
    :else (mk-map* {:cte t} body)))

(defn with* [m [a1 a2 a3 a4 a5 :as xs]]
  (cond
    (empty? xs) {:with m}
    (and (:sql-stmt a1) (nil? a2))
    (merge a1 {:with m})
    (:cte a1) (recur (conj-in m [:ctes] a1) (rest xs))
    ; (cte :tbl (as :t))
    (alias? a2) (recur (conj-in m [:ctes] (cte a1 a2)) (c/drop 2 xs))
    ; (cte :tbl :as :t) or (cte :tbl [:c1 :c2] (as :t))
    (or (as? a2) (alias? a3)) (recur (conj-in m [:ctes] (cte a1 a2 a3)) (c/drop 3 xs))
    ; (cte :tbl [:c1 :c2] :as :t) or (cte :tbl :columns [:c1 :c2] (as :t))
    (or (as? a3) (alias? a4)) (recur (conj-in m [:ctes] (cte a1 a2 a3 a4)) (c/drop 4 xs))
    ; (cte :tbl :columns [:c1 :c2] as :t)
    (as? a4) (recur (conj-in m [:ctes] (cte a1 a2 a3 a4 a5)) (c/drop 5 xs))
    :else (throw (RuntimeException. "Illegal with clause"))))

(defn with [& body]
  (with* {:ctes []} body))

(defn with-recursive [& body]
  (with* {:ctes [] :recursive true} body))

(defn not-indexed [] {:indexed-by nil})

(defn from [& [tbl aka & rem-xs :as xs]]
  (cond
    (alias? aka) {:from (cons (merge {:source tbl} aka) rem-xs)}
    (as? aka) {:from (cons {:source tbl :as (first rem-xs)} (rest rem-xs))}
    (sequential? tbl) {:from (cons {:source tbl} (rest xs))}
    :else {:from xs}))

(defmacro mk-join-fns [& xs]
  (cons 'do
        (for [x xs]
          (let [jn (str (name x) "-join")]
            `(defn ~(symbol jn) ~'[src & body]
               (mk-map* {:source ~'src :op ~(keyword jn)} ~'body))))))

(mk-join-fns :cross :inner :left :right :full
             :left-outer :right-outer :full-outer
             :natural :natural-cross :natural-inner
             :natural-left :natural-right :natural-full
             :natural-left-outer :natural-right-outer :natural-full-outer)

(defn join [src & body]
  (mk-map* {:source src :op :join} body))

(defn straight-join [src & body]
  (mk-map* {:source src :op :straight_join} body))

(defn src [src & body]
  (mk-map* {:source src} body))

(defn comma-join [src & body]
  (mk-map* {:source src :op ","} body))

(defn nil-join [src & body]
  (mk-map* {:source src :op nil} body))

(defn using [& xs] {:using xs})

(defn select [flds & body]
  (let [stmt (cond
               (sql-stmt? flds) flds
               (fields? flds) (merge {:sql-stmt :select} flds)
               :else {:sql-stmt :select
                      :fields (fields* [] (sql/as-coll flds))})]
    (mk-map* stmt body)))

(defn select-from [tbl & body]
  (apply select :* (from tbl) body))

;;; Select Query modifiers

(defn all [] (modifier :all))
(defn distinct [] (modifier :distinct))

(defn select-all [& xs] (merge (apply select xs) (all)))
(defn select-distinct [& xs] (merge (apply select xs) (distinct)))

(defn group-by [& xs]
  {:group-by xs})

;;; WINDOW clause

(defn windef [win & props]
  (cond
    (windef? win) (mk-map* win props)
    (keyword? win) (mk-map* {:base-win win} props)
    :else (mk-map* {:base-win nil} (cons win props))))

(defn over
  ([win]
   (cond
     (windef? win) win
     (keyword? win) {:over win}
     :else (windef win)))
  ([win & more] (apply windef win more)))

(defn win [w & [a1 a2 :as body]]
  (cond
    (:win w) (mk-map* w body)
    (alias? a1) ()
    :else (mk-map* {:win w} body)))

(defn window* [m [a1 a2 a3 a4 a5 :as xs]]
  (cond
    (empty? xs) {:window m}
    (or (raw? a1) (:win a1)) (recur (conj-in m [:wins] a1) (rest xs))
    ; (win :w (as <window-def>))
    (alias? a2) (recur (conj-in m [:wins] (win a1 a2)) (c/drop 2 xs))
    ; (win :w :as <window-def>)
    (as? a2) (recur (conj-in m [:wins] (win a1 a2 a3)) (c/drop 3 xs))
    :else (throw (RuntimeException. "Illegal window clause"))))

(defn window [& args]
  (window* {:wins []} args))

;;; Frame spec stuf

(def-props frame lo-bound hi-bound exclude)

(defn unbounded [x]
  {:bound ({:following :unbounded-following,
            :preceding :unbounded-preceding} x)})

(def unbounded-preceding {:bound :unbounded-preceding})
(def unbounded-following {:bound :unbounded-following})
(def current-row {:bound :current-row})
(defn preceding [expr] {:bound {:preceding expr}})
(defn following [expr] {:bound {:following expr}})

(defn bounds
  ([lo] (bounds lo current-row))
  ([lo hi] {:lo-bound (:bound lo) :hi-bound (:bound hi)}))

(def exclude-no-others {:exclude :no-others})
(def exclude-current-row {:exclude :current-row})
(def exclude-group {:exclude :group})
(def exclude-ties {:exclude :ties})

;;; ORDER BY clause

(defn order-by [& xs]
  {:order-by (vec xs)})

(def-entity-maps {:id :expr} order-term)
(def-props collate nulls order)
(def ASC {:order :asc})
(def DESC {:order :desc})
(def NULLS-FIRST {:nulls :first})
(def NULLS-LAST {:nulls :last})

(defn asc [& [x & body :as args]]
  (cond
    (nil? args) {:order :asc}
    (or (name? x) (integer? x) (sequential? x)) (mk-map {:expr x} body {:order :asc})
    :else (mk-map args {:order :asc})))

(defn desc [& [x & body :as args]]
  (cond
    (nil? args) {:order :desc}
    (or (name? x) (integer? x) (sequential? x)) (mk-map {:expr x} body {:order :desc})
    :else (mk-map args {:order :desc})))

;;; LIMIT clause

(defn limit
  ([lim] {:limit lim})
  ([off lim] {:limit lim :offset off}))

;;; Compound selects

(defn selects [& xs]
  {:selects (vec xs)})

(defn compound-select* [set-op & body]
  (let [stmt (first body)
        [stmt body]
        (if (compound-select? stmt)
          [stmt (rest body)]
          (let [[sel body]
                (c/partition-by #(boolean (or (sql-stmt? %) (:values %))) body)]
            ;(println "build comp-select")
            ;(clojure.pprint/pprint sel)
            ;(clojure.pprint/pprint body)
            [{:sql-stmt :compound-select :set-op set-op :selects sel} body]))]
    (mk-map* stmt body)))

(def union (partial compound-select* :union))
(def union-all (partial compound-select* :union-all))
(def intersect (partial compound-select* :intersect))
(def intersect-all (partial compound-select* :intersect-all))
(def except (partial compound-select* :except))
(def except-all (partial compound-select* :except-all))
(def compound-select (partial compound-select* nil))

;;; DELETE statement

(defn delete
  "With one or more args, creates a DELETE statement. With no args, specifies
  what database operation fires a TRIGGER."
  ([] {:op :delete})
  ([stmt & body]
   (let [[stmt body] (cond
                       (sql-stmt? stmt) [stmt body]
                       (or (= :from stmt) (:from stmt))
                       [{:sql-stmt :delete} (cons stmt body)]
                       :else [{:sql-stmt :delete :from stmt} body])]
     (mk-map* stmt body))))

(def delete-from delete)

;;; INSERT statement

(defn insert
  "With one or more args, creates an INSERT statement. With no args, specifies
  what database operation fires a TRIGGER."
  ([] {:op :insert})
  ([stmt & [cols & rem-body :as body]]
   (cond
     (sql-stmt? stmt) (mk-map* stmt body)
     (= :into stmt) (apply insert body)
     (:into stmt) (apply insert (:into stmt) body)
     (vector? cols) (apply insert stmt :columns cols rem-body)
     :else (mk-map* {:sql-stmt :insert :into stmt :op :insert}
                    (map #(if (select? %) {:values %} %) body)))))

(def insert-into insert)

(defn replace-into [stmt & body]
  (merge (apply insert stmt body) {:op :replace}))

(defn insert-or-rollback [stmt & body]
  (merge (apply insert stmt body) {:op :insert-or-rollback}))

(defn insert-or-abort [stmt & body]
  (merge (apply insert stmt body) {:op :insert-or-abort}))

(defn insert-or-replace [stmt & body]
  (merge (apply insert stmt body) {:op :insert-or-replace}))

(defn insert-or-fail [stmt & body]
  (merge (apply insert stmt body) {:op :insert-or-fail}))

(defn insert-or-ignore [stmt & body]
  (merge (apply insert stmt body) {:op :insert-or-ignore}))

(defn value [& [x & xx :as xs]]
  (cond
    (:values x) (mk-map* x xx)
    (and (select? x) (nil? xx)) {:values x}
    (= [:default] xs) {:values :default}
    :else (let [[xs ys] (split-with #(not= :-- %) xs)]
            (mk-map* {:values [xs]} (rest ys)))))

(defn values [& [x & xx :as xs]]
  (cond
    (:values x) (mk-map* x xx)
    (and (select? x) (nil? xx)) {:values x}
    (= [:default] xs) {:values :default}
    :else (let [[xs ys] (split-with vector? xs)]
            (mk-map* {:values xs} ys))))

(defn defaults [] {:columns nil :values :default})
(def default-values defaults)

;;; ON CONFLICT clause

(defn column [c & args]
  (let [col (if (:column c) c {:column c})]
    (mk-map* col args)))

(defn columns [& xs]
  {:columns (mapv #(if (vector? %) (apply column %) (column %)) xs)})

(defn on-conflict
  [oc & args]
  (cond
    (:on-conflict oc) (mk-map oc args)
    (vector? oc) {:on-conflict (apply mk-map (apply columns oc) args)}
    (or (map? oc) (seq? args)) {:on-conflict (apply mk-map oc args)}
    :else {:on-conflict oc}))

(defn do-nothing [] {:do nil})

(defn do-update [& args]
  {:do (mk-map* {} args)})

(defn on-conflict-do-nothing [] (on-conflict (do-nothing)))

(defn on-conflict-abort [] (on-conflict :abort))
(defn on-conflict-fail [] (on-conflict :fail))
(defn on-conflict-ignore [] (on-conflict :ignore))
(defn on-conflict-replace [] (on-conflict :replace))
(defn on-conflict-rollback [] (on-conflict :rollback))

;;; UPDATE statement

(defn update [stmt & body]
  (let [[stmt body] (if (sql-stmt? stmt)
                      [stmt body]
                      [{:sql-stmt :update :source stmt :op :update} body])]
    (mk-map* stmt body)))

(defn update-or-rollback [stmt & body]
  (merge (apply update stmt body) {:op :update-or-rollback}))

(defn update-or-abort [stmt & body]
  (merge (apply update stmt body) {:op :update-or-abort}))

(defn update-or-replace [stmt & body]
  (merge (apply update stmt body) {:op :update-or-replace}))

(defn update-or-fail [stmt & body]
  (merge (apply update stmt body) {:op :update-or-fail}))

(defn update-or-ignore [stmt & body]
  (merge (apply update stmt body) {:op :update-or-ignore}))

;;; EXPLAIN statements

(defn explain [stmt & body]
  (let [[stmt body] (if (= (:sql-stmt stmt) :explain)
                      [stmt body]
                      [{:sql-stmt :explain :statement stmt} body])]
    (mk-map* stmt body)))

(defn explain-query-plan [stmt & body]
  (let [[stmt body] (if (= (:sql-stmt stmt) :explain-query-plan)
                      [stmt body]
                      [{:sql-stmt :explain-query-plan :statement stmt} body])]
    (mk-map* stmt body)))

;;; ALTER TABLE statement

(declare constraints)

(defn add [c & body]
  (let [[props constr] (split-with-not constraint? body)
        constr (c/when (not-empty constr) (apply constraints constr))]
    {:add (column c props constr)}))

(defn add-column [& body]
  (merge {:column true} (apply add body)))

(defn rename
  ([new-tbl]
   {:rename (if (map? new-tbl) new-tbl {:to new-tbl})})
  ([old-col new-col]
   (let [old-col (if (map? old-col) old-col {:column old-col})
         new-col (if (map? new-col) new-col {:to new-col})]
     {:rename (merge old-col new-col)})))

(defn rename-to [new-tbl] (rename new-tbl))

(defn rename-column [old-col new-col]
  (merge {:column true} (rename old-col new-col)))

(defn alter
  [stmt & body]
  (let [[stmt body]
        (cond
          (= (:sql-stmt stmt) :alter) [stmt body]
          :else [{:sql-stmt :alter} (cons stmt body)])]
    (mk-map* stmt body)))

(defn alter-table [id & body]
  (alter (if (name? id) (table id) id) body (entity :table)))

;;; ANALYZE statement

(defn analyze
  ([] {:sql-stmt :analyze})
  ([stmt & body]
   (let [[stmt body]
         (cond
           (= (:sql-stmt stmt) :analyze) [stmt body]
           (name? stmt) [{:sql-stmt :analyze :schema stmt} body]
           :else [{:sql-stmt :analyze} (cons stmt body)])]
     (mk-map* stmt body))))

;;; ATTACH/DETACH statements

(defn attach [stmt & body]
  (let [[stmt body] (if (= (:sql-stmt stmt) :attach)
                      [stmt body]
                      [{:sql-stmt :attach :database stmt} body])]
    (mk-map* stmt body)))

(defn attach-database [& body]
  (sql (apply attach body) (modifier :database)))

(defn detach [stmt & body]
  (let [[stmt body] (if (= (:sql-stmt stmt) :detach)
                      [stmt body]
                      [{:sql-stmt :detach :as stmt} body])]
    (mk-map* stmt body)))

(defn detach-database [& body]
  (sql (apply detach body) (modifier :database)))

;;; CREATE statement

(def IF-NOT-EXISTS {:if-not-exists true})
(def WITHOUT-ROWID {:without-rowid true})

(def-bool-props
  always autoincrement if-not-exists stored temp temporary virtual without-rowid)

(def-map-props generated not-null deferrable)
(def-vec-props ctype)

;; Column CONSTRAINTs (primary-key and unique are also table constraints)

(defn constraint
  ([c]
   (if (:constraint c) c (constraint nil c)))
  ([id c]
   {:id id, :constraint c}))

(defn constraints [& xs]
  {:constraints (mapv #(if (vector? %) (apply constraint %) (constraint %)) xs)})

(defn primary-key [& [cols & body]]
  (cond
    (vector? cols) {:primary-key (mk-map (apply columns cols) body)}
    :else {:primary-key (apply mk-map {} cols body)}))

(defn unique [& [cols & body]]
  (cond
    (vector? cols) {:unique (mk-map (apply columns cols) body)}
    :else {:unique (apply mk-map {} cols body)}))

(defn generated-always [& body]
  (generated body (always)))

(defn generated-as [expr & body]
  (generated (as expr) body))

(defn generated-always-as [expr & body]
  (generated (as expr) body (always)))

(def AUTOINCREMENT (autoincrement))
(def NOT-NULL (not-null))
(def PRIMARY-KEY (primary-key))
(def UNIQUE (unique))

(def STORED (stored))
(def VIRTUAL (virtual))

(defn on-delete-set-null [] (on-delete :set-null))
(defn on-delete-set-default [] (on-delete :set-default))
(defn on-delete-cascade [] (on-delete :cascade))
(defn on-delete-restrict [] (on-delete :restrict))
(defn on-delete-no-action [] (on-delete :no-action))

(defn on-update-set-null [] (on-update :set-null))
(defn on-update-set-default [] (on-update :set-default))
(defn on-update-cascade [] (on-update :cascade))
(defn on-update-restrict [] (on-update :restrict))
(defn on-update-no-action [] (on-update :no-action))

(defn deferrable-initially-deferred [] (deferrable (initially :deferred)))
(defn deferrable-initially-immediate [] (deferrable (initially :immediate)))
(defn not-deferrable [& body] (deferrable body (modifier :not)))
(defn not-deferrable-initially-deferred []
  (deferrable (initially :deferred) (modifier :not)))
(defn not-deferrable-initially-immediate []
  (deferrable (initially :immediate) (modifier :not)))

(defn references [t & [cols & args :as body]]
  (let [[m body]
        (cond
          (:references t) [(:references t) body]
          (map? t) [t body]
          (vector? cols) [(merge {:table t} (apply columns cols)) args]
          :else [{:table t} body])]
    {:references (mk-map* m body)}))

;; Table CONSTRAINTS

(defn foreign-key [& [cols & body]]
  (cond
    (vector? cols) {:foreign-key (mk-map (apply columns cols) body)}
    :else {:foreign-key (apply mk-map {} cols body)}))

;; Generic CREATE syntax

(defn create
  [stmt & body]
  (let [[stmt body]
        (cond
          (= (:sql-stmt stmt) :create) [stmt body]
          :else [{:sql-stmt :create} (cons stmt body)])]
    (mk-map* stmt body)))

(defn create-if-not-exists [& body]
  (create body (if-not-exists)))

(defn create-temp [& body]
  (create body (temp)))

(defn create-temp-if-not-exists [& body]
  (create-temp body (if-not-exists)))

(defn create-temporary [& body]
  (create body (temporary)))

(defn create-temporary-if-not-exists [& body]
  (create-temporary body (if-not-exists)))

(defn split-constraints
  "Splits out trailing table constraints from `xs`. Returns both parts of split"
  [xs]
  (loop [xs (vec xs), ys ()]
    (let [c (last xs)]
      (if (table-constraint? c)
        (recur (pop xs) (cons c ys))
        [xs ys]))))

(defn col-defs [xs]
  (let [[cdefs tbl-constraints] (split-constraints xs)
        cdefs (flatten cdefs)
        col-name? #(or (keyword? %) (symbol? %) (:column %))]
    (loop [args [], [c & cs :as cdefs] cdefs]
      (cond
        (empty? cdefs) (mk-map (apply columns args)
                               (apply constraints tbl-constraints))
        (:column c) (recur (conj args c) cs)
        (name? c)
        (let [[props more] (split-with-not col-name? cs)
              [props constr] (split-with-not constraint? props)
              constr (c/when (not-empty constr) (apply constraints constr))]
          (recur (conj args (column c props constr)) more))
        :else (throw (RuntimeException. "Expected column name or definition"))))))

(def BLOB (ctype :blob))
(def INTEGER (ctype :integer))
(def NUMERIC (ctype :numeric))
(def REAL (ctype :real))
(def TEXT (ctype :text))

(defn create-table
  [id & body]
  (if (name? id)
    (let [body (if (vector? (first body))
                 [(col-defs (first body)) (rest body)]
                 body)]
      (create (table id) body (entity :table)))
    (create id body (entity :table))))

(defn create-table-if-not-exists [& body]
  (apply create-table (conj (vec body) (if-not-exists))))

(defn create-temp-table [& body]
  (apply create-table (conj (vec body) (temp))))

(defn create-temp-table-if-not-exists [& body]
  (apply create-table (conj (vec body) (temp) (if-not-exists))))

(defn create-temporary-table [& body]
  (apply create-table (conj (vec body) (temporary))))

(defn create-temporary-table-if-not-exists [& body]
  (apply create-table (conj (vec body) (temporary) (if-not-exists))))

(defn virtual-table [t]
  {:virtual true, :table t})

(defn create-virtual-table
  [id & body]
  (create (if (name? id) (table id) id) body (virtual) (entity :table)))

(defn create-virtual-table-if-not-exists [& body]
  (apply create-virtual-table (conj (vec body) (if-not-exists))))

;;; CREATE INDEX

(defn on
  ([expr] {:on expr})
  ([tbl cols] (let [cols (if (vector? cols) (apply columns cols) cols)]
                (merge {:on tbl} cols))))

(defn create-index [id & body]
  (create (if (name? id) (index id) id) body (entity :index)))

(defn create-index-if-not-exists [& body]
  (apply create-index (conj (vec body) (if-not-exists))))

(defn create-unique-index [& body]
  (apply create-index (conj (vec body) (unique))))

(defn create-unique-index-if-not-exists [& body]
  (apply create-index (conj (vec body) (unique) (if-not-exists))))

;;; CREATE TRIGGER

(def-props fire when)

(def-bool-props for-each-row)

(def-vec-props statements)

(defn fire
  ([x] {:fire x})
  ([x op] (assoc {:fire x} :op (or (:op op) op))))

(defn after
  ([] {:fire :after})
  ([op] (fire :after op)))

(defn before
  ([] {:fire :before})
  ([op] (fire :before op)))

(defn instead-of
  ([] {:fire :instead-of})
  ([op] (fire :instead-of op)))

(defn update-of [& cs] {:op {:update-of (vec cs)}})

(defn after-delete [] (after :delete))
(defn after-insert [] (after :insert))
(defn after-update-of [& cs] (after (apply update-of cs)))
(defn before-delete [] (before :delete))
(defn before-insert [] (before :insert))
(defn before-update-of [& cs] (before (apply update-of cs)))
(defn instead-of-delete [] (instead-of :delete))
(defn instead-of-insert [] (instead-of :insert))
(defn instead-of-update-of [& cs] (instead-of (apply update-of cs)))

(def AFTER-DELETE (after-delete))
(def AFTER-INSERT (after-insert))
(def BEFORE-DELETE (before-delete))
(def BEFORE-INSERT (before-insert))
(def INSTEAD-OF-DELETE (instead-of-delete))
(def INSTEAD-OF-INSERT (instead-of-insert))
(def FOR-EACH-ROW (for-each-row))

(defn create-trigger [id & body]
  (create (if (name? id) (trigger id) id) body (entity :trigger)))

(defn create-trigger-if-not-exists [& body]
  (apply create-trigger (conj (vec body) (if-not-exists))))

(defn create-temp-trigger [& body]
  (apply create-trigger (conj (vec body) (temp))))

(defn create-temp-trigger-if-not-exists [& body]
  (apply create-trigger (conj (vec body) (temp) (if-not-exists))))

(defn create-temporary-trigger [& body]
  (apply create-trigger (conj (vec body) (temporary))))

(defn create-temporary-trigger-if-not-exists [& body]
  (apply create-trigger (conj (vec body) (temporary) (if-not-exists))))

;;; CREATE VIEW

(defn create-view
  [id & body]
  (if (name? id)
    (let [body (if (vector? (first body))
                 [(apply columns (first body)) (rest body)]
                 body)]
      (create (view id) body (entity :view)))
    (create id body (entity :view))))

(defn create-view-if-not-exists [& body]
  (apply create-view (conj (vec body) (if-not-exists))))

(defn create-temp-view [& body]
  (apply create-view (conj (vec body) (temp))))

(defn create-temp-view-if-not-exists [& body]
  (apply create-view (conj (vec body) (temp) (if-not-exists))))

(defn create-temporary-view [& body]
  (apply create-view (conj (vec body) (temporary))))

(defn create-temporary-view-if-not-exists [& body]
  (apply create-view (conj (vec body) (temporary) (if-not-exists))))

;;; DROP statement

(def IF-EXISTS {:if-exists true})

(defn drop
  [stmt & body]
  (let [[stmt body]
        (cond
          (= (:sql-stmt stmt) :drop) [stmt body]
          :else [{:sql-stmt :drop} (cons stmt body)])]
    (mk-map* stmt body)))

(defn drop-if-exists [& body]
  (drop body (if-exists)))

(defn drop-index [ix & body]
  (drop (if (name? ix) (index ix) ix) body (entity :index)))

(defn drop-index-if-exists [& body]
  (apply drop-index (conj (vec body) (if-exists))))

(defn drop-table [tbl & body]
  (drop (if (name? tbl) (table tbl) tbl) body (entity :table)))

(defn drop-table-if-exists [& body]
  (apply drop-table (conj (vec body) (if-exists))))

(defn drop-trigger [trg & body]
  (drop (if (name? trg) (trigger trg) trg) body (entity :trigger)))

(defn drop-trigger-if-exists [& body]
  (apply drop-trigger (conj (vec body) (if-exists))))

(defn drop-view [v & body]
  (drop (if (name? v) (view v) v) body (entity :view)))

(defn drop-view-if-exists [& body]
  (apply drop-view (conj (vec body) (if-exists))))

;;; PRAGMA statement

(defn pragma [stmt & body]
  (let [[stmt body]
        (cond
          (= (:sql-stmt stmt) :pragma) [stmt body]
          (sequential? stmt) [{:sql-stmt :pragma, :pragma stmt} body]
          (name? stmt) (let [pragma {:sql-stmt :pragma, :pragma stmt}
                             [expr & props] body]
                         (cond
                           (map? expr) [pragma body]
                           (seq? body) [(assoc pragma :expr expr) props]
                           :else [pragma]))
          :else [{:sql-stmt :pragma} (cons stmt body)])]
    (mk-map* stmt body)))

;;; REINDEX statement

(defn reindex
  ([] {:sql-stmt :reindex})
  ([stmt & body]
   (let [[stmt body]
         (cond
           (= (:sql-stmt stmt) :reindex) [stmt body]
           (name? stmt) [{:sql-stmt :reindex :schema stmt} body]
           :else [{:sql-stmt :reindex} (cons stmt body)])]
     (mk-map* stmt body))))

;;; VACUUM statement

(defn vacuum
  ([] {:sql-stmt :vacuum})
  ([stmt & body]
   (let [[stmt body]
         (cond
           (= (:sql-stmt stmt) :vacuum) [stmt body]
           (or (:into stmt) (= :into stmt)) [{:sql-stmt :vacuum}
                                             (cons stmt body)]
           (name? stmt) [{:sql-stmt :vacuum :schema stmt} body]
           :else [{:sql-stmt :vacuum} (cons stmt body)])]
     (mk-map* stmt body))))

(defn vacuum-into
  [stmt & body]
  (let [[stmt body]
        (cond
          (= (:sql-stmt stmt) :vacuum) [stmt body]
          :else [{:sql-stmt :vacuum :into stmt} body])]
    (mk-map* stmt body)))

;;;; Transaction Control

;;; BEGIN statement

(defn begin
  "Create a SQL BEGIN statement or specify actions for CREATE TRIGGER"
  [& [stmt & body :as args]]
  (let [[stmt body]
        (cond
          (nil? args) [{:sql-stmt :begin}]
          (= (:sql-stmt stmt) :begin) [stmt body]
          (every? sql-stmt? args) [{:begin (vec args)}]
          :else [{:sql-stmt :begin} args])]
    (mk-map* stmt body)))

(defn begin-deferred [& body]
  (begin body (modifier :deferred)))

(defn begin-immediate [& body]
  (begin body (modifier :immediate)))

(defn begin-exclusive [& body]
  (begin body (modifier :exclusive)))

(defn begin-transaction [& body]
  (begin body {:transaction true}))

(defn begin-deferred-transaction [& body]
  (begin-deferred body {:transaction true}))

(defn begin-immediate-transaction [& body]
  (begin-immediate body {:transaction true}))

(defn begin-exclusive-transaction [& body]
  (begin-exclusive body {:transaction true}))

;;; COMMIT statement

(defn commit [& [stmt & body :as args]]
  (let [[stmt body]
        (cond
          (nil? args) [{:sql-stmt :commit}]
          (= (:sql-stmt stmt) :commit) [stmt body]
          :else [{:sql-stmt :commit} args])]
    (mk-map* stmt body)))

(defn commit-transaction [& body]
  (commit body {:transaction true}))

(defn end [& body]
  (commit body {:end true}))

(defn end-transaction [& body]
  (end body {:transaction true}))

;;; ROLLBACK statement

(defn to-savepoint [sp] (merge (to sp) {:savepoint true}))

(defn rollback [& [stmt & body :as args]]
  (let [[stmt body]
        (cond
          (nil? args) [{:sql-stmt :rollback}]
          (= (:sql-stmt stmt) :rollback) [stmt body]
          :else [{:sql-stmt :rollback} args])]
    (mk-map* stmt body)))

(defn rollback-transaction [& body]
  (rollback body {:transaction true}))

(defn rollback-to [stmt & body]
  (let [[stmt body]
        (cond
          (= (:sql-stmt stmt) :rollback) [stmt body]
          :else [{:sql-stmt :rollback :to stmt} body])]
    (mk-map* stmt body)))

(defn rollback-transaction-to [& body]
  (merge (apply rollback-to body) {:transaction true}))

(defn rollback-to-savepoint [& body]
  (merge (apply rollback-to body) {:savepoint true}))

(defn rollback-transaction-to-savepoint [& body]
  (merge (apply rollback-to body) {:savepoint true, :transaction true}))

;;;; Nested transactions with SAVEPOINT/RELEASE

(defn savepoint [stmt & body]
  (let [[stmt body]
        (cond
          (= (:sql-stmt stmt) :savepoint) [stmt body]
          :else [{:sql-stmt :savepoint :sp-name stmt} body])]
    (mk-map* stmt body)))

(defn release [stmt & body]
  (let [[stmt body]
        (cond
          (= (:sql-stmt stmt) :release) [stmt body]
          :else [{:sql-stmt :release :sp-name stmt} body])]
    (mk-map* stmt body)))

(defn release-savepoint [& body]
  (merge (apply release body) {:savepoint true}))

;;; Convert to string and execute

(defn to-sql [& body]
  (sql/to-sql (apply sql body)))

(defn do-sql [& body]
  (let [q (apply sql body)
        m (assoc q :sql-str (sql/to-sql q))]
    (eng/exec m)))

(def sql$ to-sql)
(def sql! do-sql)

;; Syntax to support query execution

(def-props db)

(def-vec-props params)

;; Generate <stmt>$ and <stmt>! functions for SQL statements and VALUES

(defmacro mk-render-fns [syms]
  (cons 'do
        (for [sym syms]
          `(defn ~(symbol (str (name sym) "$")) [& body#]
             (sql/to-sql (apply ~sym body#))))))

(defmacro mk-exec-fns [syms]
  (cons 'do
        (for [sym syms]
          `(defn ~(symbol (str (name sym) "!")) [& body#]
             (do-sql (apply ~sym body#))))))

(let [stmts
      '[select select-from compound-select select-distinct select-all
        union union-all intersect intersect-all except except-all
        delete delete-from
        insert insert-or-rollback insert-or-abort insert-or-replace
        insert-or-fail insert-or-ignore replace-into insert-into
        update update-or-rollback update-or-abort update-or-replace
        update-or-fail update-or-ignore
        value values with
        explain explain-query-plan
        alter alter-table
        attach attach-database detach detach-database
        analyze begin begin-transaction
        begin-deferred begin-deferred-transaction
        begin-exclusive begin-exclusive-transaction
        begin-immediate begin-immediate-transaction
        commit commit-transaction end end-transaction
        create create-if-not-exists create-temp create-temp-if-not-exists
        create-temporary create-temporary-if-not-exists
        create-index create-index-if-not-exists
        create-unique-index create-unique-index-if-not-exists
        create-table create-table-if-not-exists
        create-temp-table create-temp-table-if-not-exists
        create-temporary-table create-temporary-table-if-not-exists
        create-virtual-table create-virtual-table-if-not-exists
        create-trigger create-trigger-if-not-exists
        create-temp-trigger create-temp-trigger-if-not-exists
        create-temporary-trigger create-temporary-trigger-if-not-exists
        create-view create-view-if-not-exists
        create-temp-view create-temp-view-if-not-exists
        create-temporary-view create-temporary-view-if-not-exists
        drop drop-if-exists drop-table drop-table-if-exists
        drop-index drop-index-if-exists
        drop-trigger drop-trigger-if-exists
        drop-view drop-view-if-exists
        pragma
        reindex release release-savepoint
        rollback rollback-to rollback-to-savepoint
        rollback-transaction rollback-transaction-to rollback-transaction-to-savepoint
        savepoint
        vacuum vacuum-into]]
  (eval `(mk-render-fns ~stmts))
  (eval `(mk-exec-fns ~stmts)))

