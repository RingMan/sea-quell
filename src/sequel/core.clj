(ns sequel.core
  (:use diesel.core)
  (:require [clojure.string :as string])
  (:require [sequel.engine :as eng]))

(def-props as from limit offset)



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

(defn to-sql [stmt]
  ;; DMK TODO: turn this into multimethod based on stmt key
  (let [{:keys [fields from limit offset]} stmt
        flds (string/join ", " (map #(name (:field %)) fields))
        lim (when limit (str " LIMIT " limit))
        off (when offset (str " OFFSET " offset))
        qry (str "SELECT " flds " FROM " (name from) lim off ";")]
    qry))

(defn do-sql [stmt]
  (let [sql-str (if (:sql-stmt stmt) (to-sql stmt) stmt)]
    (eng/exec sql-str)))
