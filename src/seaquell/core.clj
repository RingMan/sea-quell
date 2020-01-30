(ns seaquell.core
  (:refer-clojure :exclude [update partition-by])
  (:require [clojure.core :as c])
  (:require [diesel.core :refer :all])
  (:require [diesel.edit :refer [conj-in]])
  (:require [seaquell [util :refer :all] [to-sql :as sql] [engine :as eng]]))

(def-props
  as binary filter-where having indexed-by modifier offset on op raw statement where)
(def-map-props set-cols)
(def set-columns set-cols)
(def set-fields set-cols)
(def set-flds set-cols)
(def-vec-props columns partition-by)

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
    (alias? a2) (recur (conj-in m [:ctes] (cte a1 a2)) (drop 2 xs))
    ; (cte :tbl :as :t) or (cte :tbl [:c1 :c2] (as :t))
    (or (as? a2) (alias? a3)) (recur (conj-in m [:ctes] (cte a1 a2 a3)) (drop 3 xs))
    ; (cte :tbl [:c1 :c2] :as :t) or (cte :tbl :columns [:c1 :c2] (as :t))
    (or (as? a3) (alias? a4)) (recur (conj-in m [:ctes] (cte a1 a2 a3 a4)) (drop 4 xs))
    ; (cte :tbl :columns [:c1 :c2] as :t)
    (as? a4) (recur (conj-in m [:ctes] (cte a1 a2 a3 a4 a5)) (drop 5 xs))
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
(defn unique [] (modifier :distinct))
(defn distinkt [] (modifier :distinct))

(defn select-all [& xs] (merge (apply select xs) (all)))
(defn select-distinct [& xs] (merge (apply select xs) (distinkt)))

(defn group [& xs]
  {:group xs})

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
    (alias? a2) (recur (conj-in m [:wins] (win a1 a2)) (drop 2 xs))
    ; (win :w :as <window-def>)
    (as? a2) (recur (conj-in m [:wins] (win a1 a2 a3)) (drop 3 xs))
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
  {:order-by xs})

(def-entity-maps {:id :expr} order-term)
(def-props collate nulls order)
(def ASC {:order :asc})
(def DESC {:order :desc})
(def NULLS-FIRST {:nulls :first})
(def NULLS-LAST {:nulls :last})

(defn asc [x]
  {:order :asc :expr x})

(defn desc [x]
  {:order :desc :expr x})

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

(defn to-sql [stmt & body]
  (sql/to-sql (mk-map* stmt body)))

(defn do-sql [stmt & body]
  {:pre [(or (string? stmt) (sql-stmt? stmt) (:values stmt))]}
  (let [m (if (string? stmt)
            (mk-map* {:sql-str stmt} body)
            (edit stmt body #(assoc % :sql-str (sql/to-sql %))))]
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
        explain explain-query-plan]]
  (eval `(mk-render-fns ~stmts))
  (eval `(mk-exec-fns ~stmts)))

