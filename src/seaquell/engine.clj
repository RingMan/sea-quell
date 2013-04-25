(ns seaquell.engine
  (:require [korma.db :as db]))

(defn exec [sql-str params results]
  (db/do-query {:sql-str sql-str :params params :results results}))
