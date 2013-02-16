(ns sequel.core
  (:use diesel.core)
  (:require [clojure.string :as string])
  (:require [sequel.engine :as eng]))

(def-props as from)


(defn sequel [] :awesome)

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
  (let [{:keys [fields from]} stmt
        flds (string/join ", " (map #(name (:field %)) fields))
        qry (str "SELECT " flds " FROM " (name from) ";")]
    qry))

(defn do-sql [stmt]
  (-> (to-sql stmt) (eng/exec)))
