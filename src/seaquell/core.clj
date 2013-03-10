(ns seaquell.core
  (:use diesel.core)
  (:require [seaquell [to-sql :as sql] [engine :as eng]]))

(def-props as having limit modifier offset on op where)

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
  (fields* [] fs))

(defn alias? [x] (and (map? x) (= (keys x) [:as])))

(defn from [& [tbl aka & rem-xs :as xs]]
  (cond
    (alias? aka) {:from (cons (merge {:source tbl} aka) rem-xs)}
    (= :as aka) {:from (cons {:source tbl :as (first rem-xs)} (rest rem-xs))}
    (sequential? tbl) {:from (cons {:source tbl} (rest xs))}
    :else {:from xs}))

(defn join [src & body]
  (mk-map* {:source src :op :join} body))

(defn src [src & body]
  (mk-map* {:source src} body))

(defn comma-join [src & body]
  (mk-map* {:source src :op ","} body))

(defn sql-stmt?
  ([x] (:sql-stmt x))
  ([stmt-type x] (= (:sql-stmt x) stmt-type)))

(def select? (partial sql-stmt? :select))

(defn select [flds & body]
  (let [stmt (if (sql-stmt? flds)
               flds
               {:sql-stmt :select
                :fields (apply fields (sql/as-coll flds))})]
    (mk-map* stmt body)))

(defn sel-expr [& xs]
  (select (field xs)))

(defn sel-* [tbl & body]
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
          (let [[sel body] (partition-by (comp boolean sql-stmt?) body)]
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

;;; Convert to string and execute

(def to-sql sql/to-sql)

(defn select$ [& body]
  (to-sql (apply select body)))

(defn sel$-expr [& body]
  (to-sql (apply sel-expr body)))

(defn sel$-* [& body]
  (to-sql (apply sel-* body)))

(defn do-sql [stmt]
  (let [sql-str (if (:sql-stmt stmt) (to-sql stmt) stmt)]
    (eng/exec sql-str)))

(defn select! [& body]
  (do-sql (apply select body)))

(defn sel!-expr [& body]
  (do-sql (apply sel-expr body)))

(defn sel!-* [& body]
  (do-sql (apply sel-* body)))
