(ns seaquell.core
  (:use diesel.core)
  (:require [seaquell [to-sql :as sql] [engine :as eng]]))

(def-props as having limit modifier offset on op where)

(defn field
  ([f as aka]
   (assert (= :as as))
   (field f aka))
  ([f aka] {:field f :as (or (:as aka) aka)})
  ([f] {:field f}))

(defn fields [& fs] fs)

(defn from [& xs] {:from xs})

(defn join [src & body]
  (mk-map* {:source src :op :join} body))

(defn src [src & body]
  (mk-map* {:source src} body))

(defn comma-join [src & body]
  (mk-map* {:source src :op ","} body))

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
