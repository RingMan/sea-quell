(ns seaquell.to-sql
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [seaquell.raw :as r]
            [seaquell.util :as u :refer [field? raw? windef?]]))

(defn delimit [l r x]
  (str l x r))
(def in-parens (partial delimit "(" ")"))
(def in-quotes (partial delimit "\"" "\""))
(def in-ticks (partial delimit "'" "'"))

(defn join-but-nils [sep xs]
  (string/join sep (keep identity xs)))
(def join-by-space (partial join-but-nils " "))
(def join-by-comma (partial join-but-nils ", "))

(defn as-coll [xs]
  (cond
    (map? xs) [xs]
    (coll? xs) xs
    :else [xs]))

;;; SQL Generation

(def fn-map
  {+ "+", - "-", * "*", / "/"
   < "<", <= "<=", = "=", not= "<>", >= ">=", > ">"
   not "NOT" max "MAX" min "MIN" count "COUNT" range ""
   val "" vals "" mod "MOD"})

;; DMK TODO: Think about || operator.  In MySQL, || is a logical OR but it can also
;; be used for string concatenation.  In sqlite, || is string concatenation.
(def arith-bin-ops #{"+" "-" "*" "/" "DIV" "%" "MOD" "^" "||"
                     "AND" "XOR" "OR" "<<" ">>" "&" "|" "COLLATE" "ESCAPE"})
(def rel-bin-ops #{"<" "<=" "=" "<=>" "<>" ">=" ">"
                   "IN" "NOT IN" "IS" "IS NOT" "LIKE" "NOT LIKE"
                   "GLOB" "NOT GLOB" "MATCH" "NOT MATCH" "OVERLAPS"
                   "REGEXP" "NOT REGEXP" "SOUNDS LIKE"})
(def unary-ops #{"-" "+" "~" "ALL" "ANY" "BINARY" "DATE" "EXISTS"
                 "NOT" "NOT EXISTS" "!" "SOME" "TIME" "TIMESTAMP"})
(def precedence-levels
  {0 #{"OR"}
   1 #{"XOR"}
   2 #{"AND"}
   3 #{"BETWEEN" "NOT BETWEEN" "CASE"}
   4 #{"=" "==" "<=>" "!=" "<>" "IS" "IS NOT" "IN" "NOT IN" "LIKE" "NOT LIKE"
       "GLOB" "NOT GLOB" "MATCH" "NOT MATCH" "OVERLAPS" "REGEXP" "NOT REGEXP"
       "SOUNDS LIKE"}
   5 #{"<" "<=" ">" ">="}
   6 #{"|"}
   7 #{"&"}
   8 #{"<<" ">>"}
   9 #{"+" "-"}
   10 #{"*" "/" "DIV" "%" "MOD"}
   11 #{"^"}
   12 #{"||"}
   13 #{"COLLATE" "ESCAPE"}})

(def precedence
  (reduce (fn [m [k v]]
            (let [level-map (apply hash-map (interleave v (repeat k)))]
              (merge m level-map)))
          {} precedence-levels))

(def unary-prec (inc (apply max (keys precedence-levels))))

(defn predicate? [x]
  (contains? (set/union (disj (precedence-levels 3) "CASE")
                        (precedence-levels 4)
                        (precedence-levels 5))
             x))

(def renamed-ops
  {"!=" "<>"
   "NOT=" "<>"
   "&&" "AND"
   "RANGE" ""
   "VAL" ""
   "VALS" ""})

;; DMK TODO: handle nil values when fn?
(defn normalize-fn-or-op [op]
  (if (fn? op)
    (fn-map op)
    (let [op (string/upper-case (name op))
          op (if (= op "-")
               op
               (string/join " " (string/split op #"-")))]
      (or (renamed-ops op) op))))

(declare to-sql)

(declare expr-to-sql expr-to-sql*)

(defn raw-to-sql [{:keys [raw]}]
  (cond (string? raw) raw
        (sequential? raw) (r/sql$ raw)
        :else (r/sql$ [raw])))

(defn name-to-sql [x]
  (cond
    (raw? x) (raw-to-sql x)
    (instance? java.util.regex.Pattern x)
    (string/join "." (map in-quotes (string/split (str x) #"\.")))
    :else (name x)))

(defn to-sql-keywords [x]
  (cond
    (keyword? x) (let [parts (string/split (name x) #"-")]
                   (->> parts (map string/upper-case) (string/join " ")))
    (raw? x) (raw-to-sql x)
    :else x))

(defn interval-to-sql [{:keys [interval units]}]
  (str "INTERVAL " (expr-to-sql interval) " " (to-sql-keywords units)))

(defn unary-op-to-sql [op arg]
  (str op " " (expr-to-sql* unary-prec arg)))

(defn bin-op-to-sql [parent-prec op args]
  (let [prec (precedence op)
        ;prec (dec parent-prec)
        op (str " " op " ")
        parts (map (partial expr-to-sql* prec) args)
        expr (string/join op parts)]
    (if (>= parent-prec prec) (in-parens expr) expr)))

(defn rel-op-to-sql [parent-prec op args]
  (let [prec (precedence op)
        pred-op (if (= "<>" op) "OR" "AND")
        prec (if (> (count args) 2) (precedence pred-op) prec)
        ;prec (dec parent-prec)
        pred-op (str " " pred-op " ")
        op (str " " op " ")
        parts (map (partial expr-to-sql* prec) args)
        parts (map (partial string/join op) (partition 2 1 parts))
        expr (string/join pred-op parts)]
    (if (>= parent-prec prec) (in-parens expr) expr)))

(defn fn-call-to-sql [func args]
  (let [modifier (first args)
        [modifier args] (if (#{distinct 'distinct} modifier)
                          ["DISTINCT " (rest args)]
                          [nil args])]
    (str func
         (in-parens (str modifier
                         (string/join ", " (map expr-to-sql args)))))))

(defn between-to-sql [parent-prec op [e1 e2 e3]]
  (let [expr (str (expr-to-sql* 100 e1) " " op " "
                  (expr-to-sql* 100 e2) " AND " (expr-to-sql* 100 e3))
        prec (precedence op)]
    (if (>= parent-prec prec) (in-parens expr) expr)))

(defn cast-to-sql [e t]
  (str "CAST(" (expr-to-sql e) " AS " (expr-to-sql t) ")"))

(defn else? [x]
  ;; DMK: this implementation avoids problems comparing midje metaconstants
  (and (keyword? x) (= :else x)))

(defn cases-to-sql [cases]
  (let [cases (partition 2 cases)
        when-fn (fn [[v e]]
                  (if (else? v)
                    (str "ELSE " (expr-to-sql e))
                    (str "WHEN " (expr-to-sql v)
                         " THEN " (expr-to-sql e))))
        cases (string/join " " (map when-fn cases))]
    cases))

(defn case-to-sql [v cases]
  (let [v (expr-to-sql v)
        [cases else] (if (even? (count cases))
                       [cases ""]
                       [(butlast cases)
                        (str " ELSE " (expr-to-sql (last cases)))])
        clauses (cases-to-sql cases)]
    (str "CASE " v " " clauses else " END")))

(defn cond-to-sql [cases]
  (let [cases (cases-to-sql cases)]
    (str "CASE " cases " END")))

(defn map-to-expr [m]
  (let [xs (map (fn [[k v]]
                  (if (sequential? v)
                    (let [op (normalize-fn-or-op (first v))]
                      (if (predicate? op)
                        (apply vector op k (rest v))
                        [:= k v]))
                    [:= k v]))
                m)]
    (if (> (count xs) 1) (cons :and xs) (first xs))))

(defn expr-to-sql* [prec x]
  (cond
    (nil? x) "NULL"
    (number? x) (str x)
    (true? x) "TRUE"
    (false? x) "FALSE"
    (string? x) (in-ticks x)
    (char? x) (in-ticks (str x))
    (= :select (:sql-stmt x)) (in-parens (to-sql x false))
    (raw? x) (raw-to-sql x)
    (and (map? x) (= #{:binary} (set (keys x))))
    (let [bin (:binary x)
          zero (when (odd? (count bin)) "0")]
      (str "x'" zero bin "'"))
    (and (map? x) (= #{:interval :units} (set (keys x)))) (interval-to-sql x)
    (map? x) (recur prec (map-to-expr x))
    (coll? x) (let [[op & args] x
                    op (normalize-fn-or-op op)]
                (cond
                  (and (unary-ops op) (= 1 (count args)))
                  (unary-op-to-sql op (first args))
                  (arith-bin-ops op) (bin-op-to-sql prec op args)
                  (rel-bin-ops op) (rel-op-to-sql prec op args)
                  (#{"BETWEEN" "NOT BETWEEN"} op)
                  (between-to-sql prec op args)
                  (= "CAST" op) (cast-to-sql (first args) (second args))
                  (= "CASE" op) (case-to-sql (first args) (rest args))
                  (= "COND" op) (cond-to-sql args)
                  :else (fn-call-to-sql op args)))
    :else (name-to-sql x)))

(def expr-to-sql (partial expr-to-sql* -1))

(def modifier-to-sql
  {:all "ALL "
   :distinct "DISTINCT "
   nil "" })

(def order-map
  {:asc "ASC"
   :desc "DESC"
   nil nil})

(defn order-item? [x] (:expr x))

(defn collate-clause [collate]
  (when collate (str "COLLATE " (expr-to-sql collate))))

(defn order-item [x]
  (if (order-item? x)
    (let [{:keys [expr order collate nulls]} x
          expr (expr-to-sql expr)
          order (order-map order)
          collate (collate-clause collate)
          nulls (condp = nulls
                  :first "NULLS FIRST"
                  :last "NULLS LAST"
                  nil)]
      (join-by-space [expr collate order nulls]))
    (expr-to-sql x)))

(defn order-by-clause [xs]
  (when xs
    (let [items (string/join ", " (flatten (map order-item (as-coll xs))))]
      (str "ORDER BY " items))))

(defmulti to-sql :sql-stmt)

(defn alias-to-sql [as]
  (when as (str "AS " (name-to-sql as))))

(declare windef-to-sql filter-clause)

(defn over-clause [oc]
  (cond
    (:over oc) (str "OVER " (expr-to-sql (:over oc)))
    (windef? oc) (str "OVER (" (windef-to-sql oc) ")")))

(defn field-to-sql [x]
  ;(println (format "field-to-sql called with %s" x))
  (if (field? x)
    (let [{:keys [field as filter-where]} x
          as (alias-to-sql as)
          field (expr-to-sql field)
          fc (filter-clause filter-where)
          oc (over-clause x)]
      (join-by-space [field fc oc as]))
    (expr-to-sql x)))

(defn fields-to-sql [fs]
  ;(println (format "fs is: %s\nclass of fs is: %s" fs (class fs)))
  (cond
    (nil? fs) "*"
    (map? fs) (field-to-sql fs)
    (coll? fs) (string/join ", " (map field-to-sql fs))
    :else (field-to-sql fs)))

(defn select-clause ^{:testable true}
  [modifier fields]
  (let [modifier (modifier-to-sql modifier)
        fields (fields-to-sql fields)]
    (str "SELECT " modifier fields)))

(declare join-op-to-sql)

(defn join-src-to-sql [{:keys [source as indexed-by]
                        :or {indexed-by ""} :as src}]
  (let [as (alias-to-sql as)
        indexed-by (cond
                     (nil? indexed-by) "NOT INDEXED"
                     (= "" indexed-by) nil
                     :else (str "INDEXED BY " (name-to-sql indexed-by)))]
    (cond
      (:sql-stmt source) (join-by-space [(in-parens (to-sql source false)) as])
      (sequential? source)
      (in-parens (join-by-space (map join-op-to-sql source)))
      :else (join-by-space [(name-to-sql source) as indexed-by]))))

(defn on-clause [on]
  (when on (str "ON " (expr-to-sql on))))

(defn join-op-to-sql [{:keys [source op on using] :as join}]
  (cond
    source
    (let [on (on-clause on)
          using (when using
                  (str "USING " (-> (map name-to-sql (as-coll using))
                                    (join-by-comma) (in-parens))))]
      (join-by-space [(to-sql-keywords op) (join-src-to-sql join)
                      (or on using)]))
    (:sql-stmt join) (in-parens (to-sql join false))
    :else (name-to-sql join)))

(defn as-join-op [x]
  (if (:source x) x {:source x :op ","}))

(defn from-clause [from]
  (when from
    (let [from (as-coll from)
          ops (cons (first from) (map as-join-op (rest from)))]
      (str "FROM " (string/join " " (map join-op-to-sql ops))))))

(defn where-clause [w] (when w (str "WHERE " (expr-to-sql w))))
(defn group-by-clause [group-by]
  (when group-by
    (str "GROUP BY " (string/join ", " (map expr-to-sql (as-coll group-by))))))
(defn having-clause [having]
  (when having (str "HAVING " (expr-to-sql having))))

(defn filter-clause [fw]
  (when fw (str "FILTER (" (where-clause fw) ")")))

(defn partition-by-clause [part-by]
  (when part-by
    (str "PARTITION BY "
         (string/join ", " (map expr-to-sql (as-coll part-by))))))

(defn bound-to-sql [bound]
  (cond
    (nil? bound) nil
    (:preceding bound) (str (expr-to-sql (:preceding bound)) " PRECEDING")
    (:following bound) (str (expr-to-sql (:following bound)) " FOLLOWING")
    :else (normalize-fn-or-op bound)))

(defn frame-to-sql [{:keys [frame bound lo-bound hi-bound exclude] :as f}]
  (when frame
    (let [frame (string/upper-case (name frame))
          lo (bound-to-sql (or (:bound lo-bound) lo-bound))
          hi (bound-to-sql (or (:bound hi-bound) hi-bound))
          bound (bound-to-sql bound)
          ex (when exclude (str "EXCLUDE " (normalize-fn-or-op exclude)))
          bound (if (and lo hi) (str "BETWEEN " lo " AND " hi) bound)]
      (join-by-space [frame bound ex]))))

(defn windef-to-sql [{:keys [base-win partition-by order-by] :as w}]
  (if (raw? w)
    (expr-to-sql w)
    (let [base-win (when base-win (name base-win))
          pb (partition-by-clause partition-by)
          ob (order-by-clause order-by)
          fr (frame-to-sql w)]
      (join-by-space [base-win pb ob fr]))))

(defn win-to-sql [{:keys [win as] :as w}]
  (if (raw? w)
    (expr-to-sql w)
    (str (name win) " AS " (in-parens (windef-to-sql as)))))

(defn window-clause [{:keys [wins] :as w}]
  (when w
    (str "WINDOW " (join-by-comma (map win-to-sql wins)))))

(defn limit-clause [l] (when l (str "LIMIT " (expr-to-sql l))))
(defn offset-clause [o] (when o (str "OFFSET " (expr-to-sql o))))

(defn column-to-sql [col]
  (if (:column col)
    (let [{:keys [column order collate]} col
          column (expr-to-sql column)
          order (order-map order)
          collate (collate-clause collate)]
      (join-by-space [column collate order]))
    (expr-to-sql col)))

(defn columns-to-sql [cols]
  (when cols
    (in-parens (string/join ", " (map column-to-sql (as-coll cols))))))

(defn cte-to-sql [{:keys [cte columns as]}]
  (str (name cte) (columns-to-sql columns) " AS "
       (in-parens (to-sql as false))))

(defn with-clause [{:keys [ctes recursive] :as w}]
  (when w
    (str "WITH " (when recursive "RECURSIVE ")
         (string/join ", " (map cte-to-sql ctes)))))

(defn query-clauses [xs semi]
  (str (string/join " " (keep identity xs)) semi))

(defmethod to-sql :select
  ([stmt]
   (to-sql stmt true))
  ([{:keys [fields modifier from where group-by having window
            order-by limit offset with] :as stmt}
    semi?]
   (let [with (with-clause with)
         select (select-clause modifier fields)
         from (from-clause from)
         where (where-clause where)
         group-by (group-by-clause group-by)
         having (having-clause having)
         window (window-clause window)
         order-by (order-by-clause order-by)
         limit (limit-clause limit)
         offset (offset-clause offset)
         semi (when semi? ";")]
     (query-clauses
       [with select from where group-by having window order-by limit offset] semi))))

(defn values-to-sql [values]
  (cond
    (= :default values) "DEFAULT VALUES"
    (:sql-stmt values) (to-sql values false)
    :else (str "VALUES "
               (string/join
                 ", "
                 (map #(in-parens (string/join ", " (map expr-to-sql %)))
                      values)))))

(defmethod to-sql :default
  ([x _]
   (if (:values x)
     (values-to-sql (:values x))
     (throw (RuntimeException. (str "to-sql [x _] not implemented for "
                                    (:sql-stmt x) " statement")))))
  ([x]
   (if (:values x)
     (str (values-to-sql (:values x)) ";")
     (throw (RuntimeException. (str "to-sql [x] not implemented for "
                                    (:sql-stmt x) " statement"))))))

(defmethod to-sql :compound-select
  ([stmt] (to-sql stmt true))
  ([{:keys [set-op selects order-by limit offset prepend-op? with] :as stmt} semi?]
  (let [with (with-clause with)
        set-op (to-sql-keywords set-op)
        sep (if set-op (str " " set-op " ") " ")
        selects (if-not prepend-op?
                  (cons (first selects)
                        (map #(assoc % :prepend-op? true) (rest selects)))
                  selects)
        select-ops (string/join sep (map #(to-sql % false) selects))
        select-ops (if prepend-op? (str set-op " " select-ops) select-ops)
        order-by (order-by-clause order-by)
        limit (limit-clause limit)
        offset (offset-clause offset)
        semi (when semi? ";")]
    (query-clauses [with select-ops order-by limit offset] semi))))

(defmethod to-sql :delete
  ([{:keys [from where order-by limit offset with] :as stmt}]
   (let [with (with-clause with)
         from (if (sequential? from) (first from) from)
         from (merge (select-keys stmt [:as :indexed-by])
                     (if (map? from) from {:source from}))
         delete (str "DELETE FROM " (join-src-to-sql from))
         where (where-clause where)
         order-by (order-by-clause order-by)
         limit (limit-clause limit)
         offset (offset-clause offset)]
     (query-clauses [with delete where order-by limit offset] ";"))))

(declare set-clause)

(defn do-clause [action]
  (case action
    (nil :nothing) "DO NOTHING"
    (let [{set-c :set :keys [where]} action
          set-c (set-clause set-c)
          where (where-clause where)]
      (query-clauses ["DO UPDATE" set-c where] nil))))

(defn on-conflict-clause [on-conflict]
  (if (keyword? on-conflict)
    (str "ON CONFLICT " (to-sql-keywords on-conflict))
    (when-let [{action :do :keys [columns where]} on-conflict]
      (let [columns (columns-to-sql columns)
            where (where-clause where)
            action (do-clause action)]
        (query-clauses ["ON CONFLICT" columns where action] nil)))))

(defmethod to-sql :insert
  ([{:keys [op into as columns values on-conflict with] :as stmt}]
   (let [with (with-clause with)
         insert (str (to-sql-keywords op) " INTO " (expr-to-sql into))
         as (alias-to-sql as)
         columns (columns-to-sql columns)
         values  (values-to-sql values)
         on-conflict (on-conflict-clause on-conflict)]
     (query-clauses [with insert as columns values on-conflict] ";"))))

(defn set-clause [set-c]
  (when set-c
    (str "SET "
         (string/join
           ", "
           (map (fn [[k v]] (str (expr-to-sql k) "=" (expr-to-sql v)))
                (sort set-c))))))

(defmethod to-sql :update
  ([{set-c :set :keys [op from where order-by limit offset with] :as stmt}]
   (let [with (with-clause with)
         update (str (to-sql-keywords op) " " (join-src-to-sql stmt))
         set-c (set-clause set-c)
         from (from-clause from)
         where (where-clause where)
         order-by (order-by-clause order-by)
         limit (limit-clause limit)
         offset (offset-clause offset)]
     (query-clauses [with update set-c from where order-by limit offset] ";"))))

(defmethod to-sql :explain [stmt]
  (str "EXPLAIN " (to-sql (:statement stmt))))

(defmethod to-sql :explain-query-plan [stmt]
  (str "EXPLAIN QUERY PLAN " (to-sql (:statement stmt))))

(declare column-def-to-sql)

(defmethod to-sql :alter
  ([{:keys [add column entity rename table] :as stmt}]
   (let [alter-s "ALTER"
         entity (if entity
                  entity
                  (cond
                    table :table
                    :else :to-sql/no-entity-to-alter))
         tgt (expr-to-sql (get stmt entity))
         entity (when entity (-> entity name string/upper-case))
         add (when add (str "ADD " (when column "COLUMN ")
                            (column-def-to-sql add)))
         rename
         (when rename
           (join-by-space
             ["RENAME" (when column "COLUMN")
              (when (:column rename) (name-to-sql (:column rename)))
              (when (:to rename) (str "TO " (name-to-sql (:to rename))))]))]
     (query-clauses [alter-s entity tgt rename add] ";"))))

(defmethod to-sql :analyze
  ([{:keys [schema into] :as stmt}]
   (let [analyze "ANALYZE"
         schema (when schema (expr-to-sql schema))]
     (query-clauses [analyze schema] ";"))))

(defmethod to-sql :attach
  ([{:keys [modifier database as] :as stmt}]
   (let [attach "ATTACH"
         modifier (when modifier (-> modifier name string/upper-case))
         database (expr-to-sql (name database))
         as (alias-to-sql as)]
     (query-clauses [attach modifier database as] ";"))))

(defmethod to-sql :begin
  ([{:keys [modifier transaction] :as stmt}]
   (let [begin "BEGIN"
         modifier (when modifier (-> modifier name string/upper-case))
         transaction (when transaction "TRANSACTION")]
     (query-clauses [begin modifier transaction] ";"))))

(defmethod to-sql :commit
  ([{:keys [end transaction] :as stmt}]
   (let [commit (if end "END" "COMMIT")
         transaction (when transaction "TRANSACTION")]
     (query-clauses [commit transaction] ";"))))

(defmethod to-sql :detach
  ([{:keys [modifier as] :as stmt}]
   (let [detach "DETACH"
         modifier (when modifier (-> modifier name string/upper-case))
         as (expr-to-sql as)]
     (query-clauses [detach modifier as] ";"))))

(defn using-clause [using]
  (when-let [[module & args] (seq using)]
    (str "USING " (name-to-sql module)
         (when args
           (str "(" (string/join ", " (map expr-to-sql args)) ")")))))

(defn ctype-to-sql [ctype]
  (when ctype
    (let [[ids nums] (u/split-with-not number? ctype)
          ids (join-by-space
                (map #(if (keyword? %)(to-sql-keywords %) (expr-to-sql %)) ids))
          nums (when (not-empty nums)
                 (in-parens (join-by-comma (map expr-to-sql nums))))]
      (str ids nums))))

(defn not-null-to-sql [{:keys [on-conflict]}]
  (let [on-conflict (on-conflict-clause on-conflict)]
    (query-clauses ["NOT NULL" on-conflict] nil)))

(defn pk-or-unique-to-sql [prefix {:keys [columns order on-conflict autoincrement]}]
  (let [columns (when columns (columns-to-sql columns))
        order (order-map order)
        on-conflict (on-conflict-clause on-conflict)
        autoincrement (when autoincrement "AUTOINCREMENT")]
    (query-clauses [prefix columns order on-conflict autoincrement] nil)))

(defn deferrable-clause [{:keys [initially modifier] :as deferrable}]
  (when deferrable
    (let [initially (when initially (str "INITIALLY " (to-sql-keywords initially)))
          modifier (when modifier (to-sql-keywords modifier))]
      (join-by-space [modifier "DEFERRABLE" initially]))))

(defn references-to-sql [{:keys [table columns match on-delete on-update deferrable]}]
  ;TODO: maybe allow clauses key that can contain multiple match, on-delete,
  ;or on-update clauses in an arbitrary order as SQLite syntax allows
  (let [table (name-to-sql table)
        columns (columns-to-sql columns)
        on-delete (when on-delete (str "ON DELETE " (to-sql-keywords on-delete)))
        on-update (when on-update (str "ON UPDATE " (to-sql-keywords on-update)))
        match (when match (str "MATCH (" (to-sql-keywords match) ")"))
        deferrable (deferrable-clause deferrable)]
    (join-by-space ["REFERENCES" table columns on-delete on-update match deferrable])))

(defn foreign-key-clause [{:keys [columns references]}]
  (let [columns (columns-to-sql columns)
        refs (references-to-sql references)]
    (join-by-space ["FOREIGN KEY" columns refs])))

(defn generated-clause [{:keys [always as stored virtual]}]
  (let [always (when always "GENERATED ALWAYS")
        as (when as (str "AS (" (expr-to-sql as) ")"))
        storage (cond stored "STORED", virtual "VIRTUAL")]
    (join-by-space [always as storage])))

(defn constraint-to-sql
  [{{:keys [check collate default foreign-key generated not-null primary-key
            references unique] :as c} :constraint,
    id :id}]
  (let [c-str
        (cond
          (keyword? c) (to-sql-keywords c)
          check (str "CHECK (" (expr-to-sql check) ")")
          collate (collate-clause collate)
          (contains? c :default) (str "DEFAULT " (expr-to-sql default))
          foreign-key (foreign-key-clause foreign-key)
          generated (generated-clause generated)
          not-null (not-null-to-sql not-null)
          primary-key (pk-or-unique-to-sql "PRIMARY KEY" primary-key)
          unique (pk-or-unique-to-sql "UNIQUE" unique)
          references (references-to-sql references))
        id (when id (str "CONSTRAINT " (name-to-sql id)))]
    (join-by-space [id c-str])))

(defn constraints-to-sql [xs join-fn]
  (when-not (empty? xs)
    (join-fn (map constraint-to-sql xs))))

(defn column-def-to-sql [col]
  (if (:column col)
    (let [{:keys [column ctype constraints order collate]} col
          column (expr-to-sql column)
          ctype (ctype-to-sql ctype)
          constraints (constraints-to-sql constraints join-by-space)
          order (order-map order)
          collate (collate-clause collate)]
      (join-by-space [column ctype constraints collate order]))
    (expr-to-sql col)))

(defn column-defs-to-sql [cols]
  (when cols
    (string/join ", " (map column-def-to-sql (as-coll cols)))))

(defn db-entity [ent idx tbl trg view]
  (if ent
    ent
    (cond idx :index
          tbl :table
          trg :trigger
          view :view
          :else :to-sql/invalid-db-entity)))

(defn trigger-ev-to-sql [{:keys [fire op] :as stmt}]
  (when op
    (let [fire (when fire (to-sql-keywords fire))
          {:keys [update-of]} op]
      (join-by-space
        [fire
         (if update-of
           (str "UPDATE OF " (join-by-comma (map name-to-sql update-of)))
           (to-sql-keywords op))]))))

(defn begin-clause [begin]
  (when begin
    (str "BEGIN " (join-by-space (map to-sql begin)) " END")))

(defmethod to-sql :create
  ([{:keys [as begin columns constraints entity for-each-row index on table
            trigger view if-not-exists temp temporary unique using virtual
            where without-rowid] when-e :when, :as stmt}]
   (let [create "CREATE"
         begin (begin-clause begin)
         columns (column-defs-to-sql columns)
         constraints (constraints-to-sql constraints join-by-comma)
         columns (when columns (in-parens (join-by-comma [columns constraints])))
         entity (db-entity entity index table trigger view)
         ent (when entity (-> entity name string/upper-case))
         ev (trigger-ev-to-sql stmt)
         for-each-row (when for-each-row "FOR EACH ROW")
         id (expr-to-sql (get stmt entity))
         if-not-exists (when if-not-exists "IF NOT EXISTS")
         temp (when temp "TEMP")
         temporary (when temporary "TEMPORARY")
         tmp (or temp temporary)
         unique (when unique "UNIQUE")
         on (on-clause on)
         using (using-clause using)
         virtual (when virtual "VIRTUAL")
         when-e (when (contains? stmt :when) (str "WHEN " (expr-to-sql when-e)))
         where (where-clause where)
         without-rowid (when without-rowid "WITHOUT ROWID")
         as (when as (str "AS " (to-sql as nil)))
         clauses
         (case entity
           :index [create unique ent if-not-exists id on columns where]
           (:table :view)
           (if virtual
             [create virtual ent if-not-exists id using]
             [create tmp ent if-not-exists id columns without-rowid as])
           :trigger
           [create tmp ent if-not-exists id ev on for-each-row when-e begin])]
     (query-clauses clauses ";"))))

(defmethod to-sql :drop
  ([{:keys [entity index table trigger view if-exists] :as stmt}]
   (let [drop-s "DROP"
         entity (if entity
                  entity
                  (cond
                    index :index
                    table :table
                    trigger :trigger
                    view :view
                    :else :to-sql/no-entity-to-drop))
         tgt (expr-to-sql (get stmt entity))
         entity (when entity (-> entity name string/upper-case))
         if-exists (when if-exists "IF EXISTS")]
     (query-clauses [drop-s entity if-exists tgt] ";"))))

(defmethod to-sql :pragma
  ([{:keys [pragma expr] :as stmt}]
   (let [expr (expr-to-sql (if (and pragma (contains? stmt :expr))
                             [= pragma expr]
                             (or pragma expr)))]
     (query-clauses ["PRAGMA" expr] ";"))))

(defmethod to-sql :reindex
  ([{:keys [schema into] :as stmt}]
   (let [reindex "REINDEX"
         schema (when schema (expr-to-sql schema))]
     (query-clauses [reindex schema] ";"))))

(defmethod to-sql :release
  ([{:keys [savepoint sp-name] :as stmt}]
   (let [release "RELEASE"
         savepoint (when savepoint "SAVEPOINT")
         sp-name (expr-to-sql sp-name)]
     (query-clauses [release savepoint sp-name] ";"))))

(defmethod to-sql :rollback
  ([{:keys [transaction to savepoint] :as stmt}]
   (let [rollback "ROLLBACK"
         transaction (when transaction "TRANSACTION")
         savepoint (when savepoint "SAVEPOINT ")
         to (when to (str "TO " savepoint (expr-to-sql to)))]
     (query-clauses [rollback transaction to] ";"))))

(defmethod to-sql :savepoint
  ([{:keys [sp-name] :as stmt}]
   (let [savepoint "SAVEPOINT"
         sp-name (expr-to-sql sp-name)]
     (query-clauses [savepoint sp-name] ";"))))

(defmethod to-sql :vacuum
  ([{:keys [schema into] :as stmt}]
   (let [vacuum "VACUUM"
         schema (when schema (expr-to-sql schema))
         into (when into (str "INTO " (expr-to-sql into)))]
     (query-clauses [vacuum schema into] ";"))))

(defmethod to-sql :sql [stmt]
  (r/sql$ (:tokens stmt)))

