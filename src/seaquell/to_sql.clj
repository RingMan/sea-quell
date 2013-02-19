(ns seaquell.to-sql
  (:require [clojure.string :as string]))

;;; SQL Generation

(def modifier-map
  {:all "ALL "
   :distinct "DISTINCT "
   nil "" })

(def order-map
  {:asc " ASC"
   :desc " DESC"
   nil "" })

(defn order-item [x]
  (let [order (order-map (:order x))]
    (cond
      (:expr x) (map #(str (name %) order) (:expr x))
      :else (name x))))

(defn order-by-clause [xs]
  (when xs
    (let [items (string/join ", " (flatten (map order-item xs)))]
      (str "ORDER BY " items))))

(defmulti to-sql :sql-stmt)

(defmethod to-sql :default [x]
  (throw (RuntimeException. (str "to-sql not implemented for "
                                 (:sql-stmt x) " statement"))))

(defn select-clause ^{:testable true}
  [modifier fields]
  (let [modifier (modifier-map modifier)
        fields (string/join ", " (map #(name (:field %)) fields))]
    (str "SELECT " modifier fields)))

(defn from-clause [from]
  (when from (str "FROM " (name from))))
(defn where-clause [w] (when w (str "WHERE " w)))
(defn group-clause [group]
  (when group
    (str "GROUP BY " (string/join ", " (map #(name %) group)))))
(defn having-clause [having]
  (when having (str "HAVING " having)))
(defn limit-clause [l] (when l (str "LIMIT " l)))
(defn offset-clause [o] (when o (str "OFFSET " o)))

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

