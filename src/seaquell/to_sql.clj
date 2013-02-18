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

(defn order-clause [xs]
  (when xs
    (let [items (string/join ", " (flatten (map order-item xs)))]
      (str " ORDER BY " items))))

(defmulti to-sql :sql-stmt)

(defmethod to-sql :default [x]
  (throw (RuntimeException. (str "to-sql not implemented for "
                                 (:sql-stmt x) " statement"))))

(defmethod to-sql :select [stmt]
  ;; DMK TODO: turn this into multimethod based on stmt key
  (let [{:keys [fields modifier from where group having
                order-by limit offset]} stmt
        modifier (modifier-map modifier)
        flds (string/join ", " (map #(name (:field %)) fields))
        from (when from (str " FROM " (name from)))
        where (when where (str " WHERE " where))
        group (when group
                (str " GROUP BY " (string/join ", " (map #(name %) group))))
        having (when having
                 (str " HAVING " having))
        ord (order-clause order-by)
        lim (when limit (str " LIMIT " limit))
        off (when offset (str " OFFSET " offset))
        qry (str "SELECT " modifier flds from where group having
                 ord lim off ";")]
    qry))
