(ns seaquell.core
  (:use diesel.core)
  (:require [clojure.string :as string])
  (:require [seaquell.engine :as eng]))

(def-props as from limit offset where)



(defn field [f]
  ;; DMK TODO: break apart string or keyword db.table.field
  (if (:field f) f {:field f}))

(defn fields [& fs] (map field fs))

(defn select [flds & body]
  (mk-map*
    {:sql-stmt :select
     :fields (cond
               (coll? flds) (map field flds)
               :else [(field flds)])}
    body))

;;; ORDER BY clause

(defn order-by [& xs]
  {:order-by xs})

(defn asc [& xs]
  {:order :asc :expr xs})

(defn desc [& xs]
  {:order :desc :expr xs})

;;; SQL Generation

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

(defn to-sql [stmt]
  ;; DMK TODO: turn this into multimethod based on stmt key
  (let [{:keys [fields from where order-by limit offset]} stmt
        flds (string/join ", " (map #(name (:field %)) fields))
        from (when from (str " FROM " (name from)))
        where (when where (str " WHERE " where))
        ord (order-clause order-by)
        lim (when limit (str " LIMIT " limit))
        off (when offset (str " OFFSET " offset))
        qry (str "SELECT " flds from where ord lim off ";")]
    qry))

(defn do-sql [stmt]
  (let [sql-str (if (:sql-stmt stmt) (to-sql stmt) stmt)]
    (eng/exec sql-str)))
