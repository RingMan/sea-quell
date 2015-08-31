(ns seaquell.core
  (:refer-clojure :exclude [update])
  (:require [diesel.core :refer :all])
  (:require [diesel.edit :refer :all])
  (:require [seaquell [to-sql :as sql] [engine :as eng]]))

(def-props as binary having indexed-by modifier offset on op raw where)
(def-map-props set-cols)
(def set-columns set-cols)
(def-vec-props columns)

(defn field
  ([f as aka]
   (assert (= :as as))
   (field f aka))
  ([f aka] (merge {:field (or (:field f) f)}
                  (when aka {:as (or (:as aka) aka)})))
  ([f] (field f nil)))

(defn fields* [acc [f aka & rem-fs :as fs]]
  (cond
    (nil? fs) acc
    (and (:as aka) (nil? (:field aka)))
    (recur (conj acc (field f (:as aka))) rem-fs)
    (= :as aka) (recur (conj acc (field f (first rem-fs))) (next rem-fs))
    :else (recur (conj acc f) (next fs))))

(defn fields [& fs]
  {:fields (fields* [] fs)})

(defn interval [ival units] {:interval ival :units units})

(defn alias? [x] (and (map? x) (= (keys x) [:as])))

(defn cte [t & [cols & rem-body :as body]]
  (cond
    (:cte t) (mk-map* t body)
    (vector? cols) (mk-map* {:cte t} (cons {:columns cols} rem-body))
    :else (mk-map* {:cte t} body)))

(defn with* [m [a1 a2 a3 a4 a5 :as xs]]
  (let [as? #(= :as %)]
    ;(println "wif " m a1 a2 a3 a4 a5)
    (cond
      (nil? a1) {:with m}
      (and (:sql-stmt a1) (nil? a2))
      (merge a1 {:with m})
      (:cte a1) (recur (conj-in m [:ctes] a1) (rest xs))
      (alias? a2) (recur (conj-in m [:ctes] (cte a1 a2)) (drop 2 xs))
      (or (as? a2) (alias? a3)) (recur (conj-in m [:ctes] (cte a1 a2 a3)) (drop 3 xs))
      (or (as? a3) (alias? a4)) (recur (conj-in m [:ctes] (cte a1 a2 a3 a4)) (drop 4 xs))
      (as? a4) (recur (conj-in m [:ctes] (cte a1 a2 a3 a4 a5)) (drop 5 xs))
      :else (throw (RuntimeException. "Illegal with clause")))))

(defn with [& body]
  (with* {:ctes []} body))

(defn with-recursive [& body]
  (with* {:ctes [] :recursive true} body))

(defn not-indexed [] {:indexed-by nil})

(defn from [& [tbl aka & rem-xs :as xs]]
  (cond
    (alias? aka) {:from (cons (merge {:source tbl} aka) rem-xs)}
    (= :as aka) {:from (cons {:source tbl :as (first rem-xs)} (rest rem-xs))}
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

(defn sql-stmt?
  ([x] (:sql-stmt x))
  ([stmt-type x] (= (:sql-stmt x) stmt-type)))

(def select? (partial sql-stmt? :select))

(defn fields? [x] (and (map? x) (= (keys x) [:fields])))

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
(defn unique [] (modifier :distinct))
(defn distinkt [] (modifier :distinct))

(defn select-all [& xs] (merge (apply select xs) (all)))
(defn select-distinct [& xs] (merge (apply select xs) (distinkt)))

(defn group [& xs]
  {:group xs})

;;; ORDER BY clause

(defn order-by [& xs]
  {:order-by xs})

(defn asc [& xs]
  {:order :asc :expr xs})

(defn desc [& xs]
  {:order :desc :expr xs})

;;; LIMIT clause

(defn limit
  ([lim] {:limit lim})
  ([off lim] {:limit lim :offset off}))

;;; Compound selects

(def compound-select? (partial sql-stmt? :compound-select))

(defn set-op? [set-op stmt]
  (and (compound-select? stmt) (= (:set-op stmt) set-op)))

(def union? (partial set-op? :union))
(def union-all? (partial set-op? :union-all))
(def intersect? (partial set-op? :intersect))
(def intersect-all? (partial set-op? :intersect-all))
(def except? (partial set-op? :except))
(def except-all? (partial set-op? :except-all))

(defn selects [& xs]
  {:selects (vec xs)})

(defn compound-select* [set-op & body]
  (let [stmt (first body)
        [stmt body]
        (if (compound-select? stmt)
          [stmt (rest body)]
          (let [[sel body]
                (partition-by #(boolean (or (sql-stmt? %) (:values %))) body)]
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

(def delete? (partial sql-stmt? :delete))

(defn delete [stmt & body]
  (let [[stmt body] (if (sql-stmt? stmt)
                      [stmt body]
                      [{:sql-stmt :delete :source stmt} body])]
    (mk-map* stmt body)))

;;; INSERT statement

(def insert? (partial sql-stmt? :insert))

(defn insert [stmt & body]
  (let [[stmt body] (if (sql-stmt? stmt)
                      [stmt body]
                      [{:sql-stmt :insert :source stmt :op :insert} body])]
    (mk-map* stmt body)))

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

(defn value [& xs] {:values [xs]})

(defn values
  ([& [x & xx :as xs]]
   (if-not (or xx (sequential? x))
     {:values x}
     {:values xs})))

(defn defaults [] {:columns nil :values :default})
(def default-values defaults)

;;; UPDATE statement

(def update? (partial sql-stmt? :update))

(defn update [stmt & body]
  (let [[stmt body] (if (sql-stmt? stmt)
                      [stmt body]
                      [{:sql-stmt :update :source stmt :op :update} body])]
    (mk-map* stmt body)))

;;; Helpers for composing queries
(defn and-expr? [x] (and (sequential? x) (#{:and 'and "and"} (first x))))
(defn or-expr? [x] (and (sequential? x) (#{:or 'or "or"} (first x))))
(defn xor-expr? [x] (and (sequential? x) (#{:xor 'xor "xor"} (first x))))

(defn and-where
  "Returns a function to AND `expr` with existing WHERE clause"
  [expr]
  (fn [q]
    (let [w (:where q)]
      (assoc q :where (if (and-expr? w) (conj (vec w) expr) [:and w expr])))))

(defn or-where
  "Returns a function to OR `expr` with existing WHERE clause"
  [expr]
  (fn [q]
    (let [w (:where q)]
      (assoc q :where (if (or-expr? w) (conj (vec w) expr) [:or w expr])))))

(defn xor-where
  "Returns a function to XOR `expr` with existing WHERE clause"
  [expr]
  (fn [q]
    (let [w (:where q)]
      (assoc q :where (if (xor-expr? w) (conj (vec w) expr) [:xor w expr])))))

(defn not-where
  "Returns a function to negate existing WHERE clause"
  []
  (fn [q]
    (let [w (:where q)]
      (assoc q :where [not w]))))

(defn and-having
  "Returns a function to AND `expr` with existing HAVING clause"
  [expr]
  (fn [q]
    (let [w (:having q)]
      (assoc q :having (if (and-expr? w) (conj (vec w) expr) [:and w expr])))))

(defn or-having
  "Returns a function to OR `expr` with existing HAVING clause"
  [expr]
  (fn [q]
    (let [w (:having q)]
      (assoc q :having (if (or-expr? w) (conj (vec w) expr) [:or w expr])))))

(defn xor-having
  "Returns a function to XOR `expr` with existing HAVING clause"
  [expr]
  (fn [q]
    (let [w (:having q)]
      (assoc q :having (if (xor-expr? w) (conj (vec w) expr) [:xor w expr])))))

(defn not-having
  "Returns a function to negate existing HAVING clause"
  []
  (fn [q]
    (let [w (:having q)]
      (assoc q :having [not w]))))

(defn cons-fields
  "Returns a function to prepend one or more fields to a query"
  [& xs]
  (fn [q]
    (let [flds (concat (:fields (apply fields xs)) (as-vec (:fields q)))]
      (assoc q :fields flds))))

(def cons-field cons-fields)

(defn conj-fields
  "Returns a function to append one or more fields to a query"
  [& xs]
  (fn [q]
    (let [flds (concat (as-vec (:fields q)) (:fields (apply fields xs)))]
      (assoc q :fields flds))))

(def conj-field conj-fields)
(def also-fields conj-fields)
(def also-field conj-fields)

(defn cons-from [& xs]
  "Returns a function to prepend `xs` to an existing FROM clause
  using the same syntax as the `from` function."
  (fn [q]
    (let [f (concat (:from (apply from xs)) (as-vec (:from q)))]
      (assoc q :from f))))

(defn conj-from
  "Returns a function to append `xs` to an existing FROM clause
  using the same syntax as the `from` function."
  [& xs]
  (fn [q]
    (let [f (concat (as-vec (:from q)) (:from (apply from xs)))]
      (assoc q :from f))))

(def also-from conj-from)

(defn cons-order-by [& xs]
  (fn [q] (assoc q :order-by (concat xs (as-vec (:order-by q))))))

(defn conj-order-by [& xs]
  (fn [q] (apply conjv-in q [:order-by] xs)))

(def also-order-by conj-order-by)

(defn cons-group [& xs]
  (fn [q] (assoc q :group (concat xs (as-vec (:group q))))))

(defn conj-group [& xs]
  (fn [q] (apply conjv-in q [:group] xs)))

(def also-group conj-group)

(defn ins-fields [n & xs]
  (fn [q]
    (let [xs (:fields (apply fields xs))
          [ws ys] (split-at n (as-vec (:fields q)))]
      (assoc q :fields (concat ws xs ys)))))

(def ins-field ins-fields)

(defn ins-from [n & xs]
  (fn [q]
    (let [xs (:from (apply from xs))
          [ws ys] (split-at n (as-vec (:from q)))]
      (assoc q :from (concat ws xs ys)))))

(defn ins-group [n & xs]
  (fn [q]
    (let [[ws ys] (split-at n (as-vec (:group q)))]
      (assoc q :group (concat ws xs ys)))))

(defn ins-order-by [n & xs]
  (fn [q]
    (let [[ws ys] (split-at n (as-vec (:order-by q)))]
      (assoc q :order-by (concat ws xs ys)))))

(defmacro mk-rm-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "rm-" (name x))) ~'[& xs]
             (fn [~'q] (apply rm-in ~'q [~(keyword x)] ~'xs))))))

(mk-rm-fns fields from group order-by)
(def rm-field rm-fields)

(defmacro mk-rm-nth-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "rm-nth-" (name x))) ~'[& xs]
             (fn [~'q] (apply rm-nth-in ~'q [~(keyword x)] ~'xs))))))

(mk-rm-nth-fns fields from group order-by)
(def rm-nth-field rm-nth-fields)

(defmacro mk-replace-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "replace-" (name x))) ~'[smap]
             (fn [~'q] (replace-in ~'q [~(keyword x)] ~'smap))))))

(mk-replace-fns fields from group order-by)
(def replace-field replace-fields)

(defmacro mk-replace-nth-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "replace-nth-" (name x))) ~'[smap]
             (fn [~'q] (replace-nth-in ~'q [~(keyword x)] ~'smap))))))

(mk-replace-nth-fns fields from group order-by)
(def replace-nth-field replace-nth-fields)

(defmacro mk-edit-nth-fns [& xs]
  (cons
    'do
    (for [x xs]
      `(defn ~(symbol (str "edit-nth-" (name x))) ~'[n f & args]
        (fn [~'q]
          (-> ~'q
              (update-in [~(keyword x)] as-vec)
              ((partial apply update-in) [~(keyword x) ~'n] ~'f ~'args)))))))

(mk-edit-nth-fns fields from group order-by)
(def edit-nth-field edit-nth-fields)

(defmacro mk-edit-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "edit-" (name x))) ~'[f & args]
             (fn [~'q] (apply update-in ~'q [~(keyword x)] ~'f ~'args))))))

(mk-edit-fns fields from where group having limit offset)

(defn rm-clauses [& ks]
  (fn [q] (apply dissoc q ks)))

(def rm-parts rm-clauses)

;;; Convert to string and execute

(def to-sql sql/to-sql)

(defn select$ [& body]
  (to-sql (apply select body)))

(defn select-from$ [& body]
  (to-sql (apply select-from body)))

(defn do-sql [stmt & body]
  {:pre [(or (string? stmt) (sql-stmt? stmt))]}
  (let [m (if (string? stmt)
            {:sql-str stmt}
            (assoc stmt :sql-str (to-sql stmt)))]
    (eng/exec (mk-map* m body))))

(defn select! [& body]
  (do-sql (apply select body)))

(defn select-from! [& body]
  (do-sql (apply select-from body)))

(defn delete$ [& body]
  (to-sql (apply delete body)))

(defn delete! [& body]
  (do-sql (apply delete body)))

(defn insert$ [& body]
  (to-sql (apply insert body)))

(defn insert! [& body]
  (do-sql (apply insert body)))

(defn update$ [& body]
  (to-sql (apply update body)))

(defn update! [& body]
  (do-sql (apply update body)))

