(ns seaquell.core
  (:use diesel.core)
  (:require [seaquell [to-sql :as sql] [engine :as eng]]))

(def-props as from having limit modifier offset where)

(defn field [f & body]
  (mk-map* {:field f} body))

(defn fields [& fs] fs)

(defn sql-stmt? [x] (:sql-stmt x))

(defn select [flds & body]
  (let [stmt (if (sql-stmt? flds)
               flds
               {:sql-stmt :select
                :fields flds})]
    (mk-map* stmt body)))

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

;;; Convert to string and execute

(def to-sql sql/to-sql)

(defn select$ [& body]
  (to-sql (apply select body)))

(defn do-sql [stmt]
  (let [sql-str (if (:sql-stmt stmt) (to-sql stmt) stmt)]
    (eng/exec sql-str)))

(defn select! [& body]
  (do-sql (apply select body)))
