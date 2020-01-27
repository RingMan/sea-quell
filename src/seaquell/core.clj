(ns seaquell.core
  (:refer-clojure :exclude [update])
  (:require [diesel.core :refer :all])
  (:require [diesel.edit :refer [conj-in]])
  (:require [seaquell [util :refer :all] [to-sql :as sql] [engine :as eng]]))

(def-props
  as binary having indexed-by modifier offset on op raw statement where window)
(def-map-props set-cols)
(def set-columns set-cols)
(def set-fields set-cols)
(def set-flds set-cols)
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
    (empty? fs) acc
    (and (:as aka) (nil? (:field aka)))
    (recur (conj acc (field f (:as aka))) rem-fs)
    (= :as aka) (recur (conj acc (field f (first rem-fs))) (next rem-fs))
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
  (let [as? #(= :as %)]
    ;(println "wif " m a1 a2 a3 a4 a5)
    (cond
      (empty? xs) {:with m}
      (and (:sql-stmt a1) (nil? a2))
      (merge a1 {:with m})
      (:cte a1) (recur (conj-in m [:ctes] a1) (rest xs))
      ; (cte :tbl (as :t))
      (alias? a2) (recur (conj-in m [:ctes] (cte a1 a2)) (drop 2 xs))
      ; (cte :tbl :as :t) or (cte :tbl [:c1 :c2] (as :t))
      (or (as? a2) (alias? a3)) (recur (conj-in m [:ctes] (cte a1 a2 a3)) (drop 3 xs))
      ; (cte :tbl [:c1 :c2] :as :t) or (cte :tbl :columns [:c1 :c2] (as :t))
      (or (as? a3) (alias? a4)) (recur (conj-in m [:ctes] (cte a1 a2 a3 a4)) (drop 4 xs))
      ; (cte :tbl :columns [:c1 :c2] as :t)
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

(defn delete [stmt & body]
  (let [[stmt body] (cond
                      (sql-stmt? stmt) [stmt body]
                      (or (= :from stmt) (:from stmt))
                      [{:sql-stmt :delete} (cons stmt body)]
                      :else [{:sql-stmt :delete :from stmt} body])]
    (mk-map* stmt body)))

(def delete-from delete)

;;; INSERT statement

(defn insert [stmt & [cols & rem-body :as body]]
  (cond
    (sql-stmt? stmt) (mk-map* stmt body)
    (= :into stmt) (apply insert body)
    (:into stmt) (apply insert (:into stmt) body)
    (vector? cols) (apply insert stmt :columns cols rem-body)
    (select? (last body)) (apply insert stmt {:values (last body)} (butlast body))
    :else (mk-map* {:sql-stmt :insert :into stmt :op :insert} body)))

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

;;; Convert to string and execute

(def to-sql sql/to-sql)

(defn do-sql [stmt & body]
  {:pre [(or (string? stmt) (sql-stmt? stmt) (:values stmt))]}
  (let [m (if (string? stmt)
            {:sql-str stmt}
            (assoc stmt :sql-str (to-sql stmt)))]
    (eng/exec (mk-map* m body))))

(def sql$ sql/to-sql)
(def sql! do-sql)

;; Syntax to support query execution

(def-props db)

(def-vec-props params)

;; Generate <stmt>$ and <stmt>! functions for SQL statements and VALUES

(defmacro mk-render-fns [syms]
  (cons 'do
        (for [sym syms]
          `(defn ~(symbol (str (name sym) "$")) [& body#]
             (to-sql (apply ~sym body#))))))

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
        explain explain-query-plan]]
  (eval `(mk-render-fns ~stmts))
  (eval `(mk-exec-fns ~stmts)))

