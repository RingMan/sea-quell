(ns seaquell.core
  (:use diesel.core)
  (:require [seaquell [to-sql :as sql] [engine :as eng]]))

(def-props as from limit modifier offset where)



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

;;; Select Query modifiers

(defn all [] (modifier :all))
(defn distinkt [] (modifier :distinct))

(defn select-all [& xs] (merge (apply select xs) (all)))
(defn select-distinct [& xs] (merge (apply select xs) (distinkt)))

;;; ORDER BY clause

(defn order-by [& xs]
  {:order-by xs})

(defn asc [& xs]
  {:order :asc :expr xs})

(defn desc [& xs]
  {:order :desc :expr xs})

;;; Convert to string and execute

(def to-sql sql/to-sql)

(defn do-sql [stmt]
  (let [sql-str (if (:sql-stmt stmt) (to-sql stmt) stmt)]
    (eng/exec sql-str)))
