(ns sequel.engine
  (:require [korma.db :as db]))

(defn exec [sql-str]
  (db/do-query {:sql-str sql-str :results :results}))
