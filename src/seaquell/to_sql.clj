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
      (str " ORDER BY " items))))

(defmulti to-sql :sql-stmt)

(defmethod to-sql :default [x]
  (throw (RuntimeException. (str "to-sql not implemented for "
                                 (:sql-stmt x) " statement"))))

(defn- where-clause [w] (when w (str " WHERE " w)))
(defn- limit-clause [l] (when l (str " LIMIT " l)))
(defn- offset-clause [o] (when o (str " OFFSET " o)))

(defmethod to-sql :select [stmt]
  ;; DMK TODO: turn this into multimethod based on stmt key
  (let [{:keys [fields modifier from where group having
                order-by limit offset]} stmt
        modifier (modifier-map modifier)
        fields (string/join ", " (map #(name (:field %)) fields))
        from (when from (str " FROM " (name from)))
        where (where-clause where)
        group (when group
                (str " GROUP BY " (string/join ", " (map #(name %) group))))
        having (when having
                 (str " HAVING " having))
        order-by (order-by-clause order-by)
        limit (limit-clause limit)
        offset (offset-clause offset)
        qry (str "SELECT " modifier fields from where group having
                 order-by limit offset ";")]
    qry))
