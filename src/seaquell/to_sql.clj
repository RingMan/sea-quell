(ns seaquell.to-sql
  (:require [clojure.string :as string]))

(defn delimit [l r x]
  (str l x r))
(def in-parens (partial delimit "(" ")"))
(def in-quotes (partial delimit "\"" "\""))
(def in-ticks (partial delimit "'" "'"))

(defn as-coll [xs]
  (cond
    (map? xs) [xs]
    (coll? xs) xs
    :else [xs]))

;;; SQL Generation

(defn to-sql-keywords [x]
  (if (keyword? x)
    (let [parts (string/split (name x) #"-")]
      (->> parts (map string/upper-case) (string/join " ")))
    x))

(def fn-map
  {+ "+", - "-", * "*", / "/"
   < "<", <= "<=", = "=", not= "<>", >= ">=", > ">"
   not "NOT" max "MAX" min "MIN" count "COUNT" val "" vals "" mod "MOD"})

;; DMK TODO: Think about || operator.  In MySQL, || is a logical OR but it can also
;; be used for string concatenation.  In sqlite, || is string concatenation.
(def arith-bin-ops #{"+" "-" "*" "/" "DIV" "%" "MOD" "^" "||" "AND" "XOR" "OR" "<<" ">>" "&" "|"})
(def rel-bin-ops #{"<" "<=" "=" "<=>" "<>" ">=" ">"
                   "IN" "NOT IN" "IS" "IS NOT" "LIKE" "NOT LIKE"
                   "GLOB" "NOT GLOB" "MATCH" "NOT MATCH" "REGEXP" "NOT REGEXP"})
(def unary-ops #{"-" "+" "~" "ALL" "ANY" "BINARY" "NOT" "!" "SOME"})
(def precedence-levels
  {0 #{"OR"}
   1 #{"XOR"}
   2 #{"AND"}
   3 #{"BETWEEN" "NOT BETWEEN" "CASE"}
   4 #{"=" "==" "<=>" "!=" "<>" "IS" "IS NOT" "IN" "NOT IN" "LIKE" "NOT LIKE"
       "GLOB" "NOT GLOB" "MATCH" "NOT MATCH" "REGEXP" "NOT REGEXP"}
   5 #{"<" "<=" ">" ">="}
   6 #{"|"}
   7 #{"&"}
   8 #{"<<" ">>"}
   9 #{"+" "-"}
   10 #{"*" "/" "DIV" "%" "MOD"}
   11 #{"^"}
   12 #{"||"}})

(def precedence
  (reduce (fn [m [k v]]
            (let [level-map (apply hash-map (interleave v (repeat k)))]
              (merge m level-map)))
          {} precedence-levels))

(def unary-prec (inc (apply max (keys precedence-levels))))

(def renamed-ops
  {"!=" "<>"
   "NOT=" "<>"
   "&&" "AND"
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
  (if (#{"EXISTS" "NOT EXISTS"} func)
    (str func (expr-to-sql (first args)))
    (str func (in-parens (string/join ", " (map expr-to-sql args))))))

(defn between-to-sql [parent-prec op [e1 e2 e3]]
  (let [expr (str (expr-to-sql* 100 e1) " " op " "
                  (expr-to-sql* 100 e2) " AND " (expr-to-sql* 100 e3))
        prec (precedence op)]
    (if (>= parent-prec prec) (in-parens expr) expr)))

(defn cast-to-sql [e t]
  (str "CAST(" (expr-to-sql e) " AS " (to-sql-keywords t) ")"))

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

;; DMK TODO: Extend the map? case to allow for equality between two arbitrary
;; expressions.  Then possbibly extend similar to Korma map predicates.

(defn expr-to-sql* [prec x]
  (cond
    (nil? x) "NULL"
    (number? x) (str x)
    (keyword? x) (name x)
    (true? x) "TRUE"
    (false? x) "FALSE"
    (string? x) (in-ticks x)
    (= :select (:sql-stmt x)) (in-parens (to-sql x false))
    (map? x) (recur prec (cons :and (map (fn [[k v]] [:= k v]) x)))
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
    :else (name x)))

(def expr-to-sql (partial expr-to-sql* -1))

(def modifier-to-sql
  {:all "ALL "
   :distinct "DISTINCT "
   nil "" })

(def order-map
  {:asc " ASC"
   :desc " DESC"
   nil "" })

(defn order-item? [x] (:expr x))

(defn order-item [x]
  (if (order-item? x)
    (let [{:keys [collate expr order]} x
          collate (when collate (str " COLLATE " (name collate)))
          order (order-map order)]
      (map #(str (expr-to-sql %) collate order) expr))
    (expr-to-sql x)))

(defn order-by-clause [xs]
  (when xs
    (let [items (string/join ", " (flatten (map order-item (as-coll xs))))]
      (str "ORDER BY " items))))

(defmulti to-sql :sql-stmt)

(defmethod to-sql :default [x]
  (throw (RuntimeException. (str "to-sql not implemented for "
                                 (:sql-stmt x) " statement"))))

(defn field-to-sql [x]
  ;(println (format "field-to-sql called with %s" x))
  (if (:field x)
    (let [{:keys [field as]} x
          as (when as (str " AS "(name as)))
          field (expr-to-sql field)]
      (str field as))
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

(defn join-but-nils [sep xs]
  (string/join sep (keep identity xs)))
(def join-by-space (partial join-but-nils " "))
(def join-by-comma (partial join-but-nils ", "))

(declare join-op-to-sql)

(defn join-src-to-sql [{:keys [source as] :as src}]
  (let [as (when as (str " AS " (name as)))]
    (cond
      (keyword? source) (str (name source) as)
      (string? source) (str source as)
      (:sql-stmt source) (str (in-parens (to-sql source false)) as)
      (coll? source) (in-parens (join-by-space (map join-op-to-sql source))))))

(defn join-op-to-sql [{:keys [source op on using] :as join}]
  (cond
    source
    (let [on (when on (str "ON " (expr-to-sql on)))
          using (when using
                  (str "USING " (-> (map name (as-coll using))
                                    (join-by-comma) (in-parens))))]
      (join-by-space [(to-sql-keywords op) (join-src-to-sql join) (or on using)]))
    (:sql-stmt join) (in-parens (to-sql join false))
    :else (name join)))

(defn from-clause [from]
  (when from
    (str "FROM " (string/join " " (map join-op-to-sql (as-coll from))))))

(defn where-clause [w] (when w (str "WHERE " (expr-to-sql w))))
(defn group-clause [group]
  (when group
    (str "GROUP BY " (string/join ", " (map expr-to-sql (as-coll group))))))
(defn having-clause [having]
  (when having (str "HAVING " (expr-to-sql having))))
(defn limit-clause [l] (when l (str "LIMIT " (expr-to-sql l))))
(defn offset-clause [o] (when o (str "OFFSET " (expr-to-sql o))))

(defn select-clauses [xs semi]
  (str (string/join " " (keep identity xs)) semi))

(defmethod to-sql :select
  ([stmt]
   (to-sql stmt true))
  ([{:keys [fields modifier from where group having
            order-by limit offset] :as stmt}
    semi?]
   (let [select (select-clause modifier fields)
         from (from-clause from)
         where (where-clause where)
         group (group-clause group)
         having (having-clause having)
         order-by (order-by-clause order-by)
         limit (limit-clause limit)
         offset (offset-clause offset)
         semi (when semi? ";")
         qry (str "SELECT " modifier fields from where group having
                  order-by limit offset semi)]
     (select-clauses [select from where group having
                      order-by limit offset] semi))))

(defmethod to-sql :compound-select
  ([stmt] (to-sql stmt true))
  ([{:keys [set-op selects order-by limit offset prepend-op?] :as stmt} semi?]
  (let [set-op (to-sql-keywords set-op)
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
    (select-clauses [select-ops order-by limit offset] semi))))
