(ns seaquell.engine
  (:require [clojure.java.jdbc :as jdbc]
            [diesel.core :refer [def-props def-vec-props def-bool-props]]
            [seaquell.util :as u]))

;; Syntax for jdbc pass-thru options

(def-bool-props as-arrays? transaction? multi?)

(def-props identifiers row-fn result-set-fn)

(defn id-fn [f] {:identifiers f})

;; Query execution using jdbc

(defn exec
  "Executes SQL CRUD operations using clojure.java.jdbc
  Takes a map with two mandatory keys:
    :db - DB spec or connection pool datasource
    :sql-str - SQL string to execute
  And one optional key:
    :params - Parameters to pass to jdbc
  The following are passed directly to jdbc/query or jdbc/execute!, respectively:
    :as-arrays? :identifiers :row-fn :result-set-fn
    :multi? :transaction?"
  [{:keys [db sql-str params] :as q}]
  {:pre [(and db sql-str)]}
  (if (or (u/select? q) (u/compound-select? q) (re-find #"^(?i)(select|values|explain) " sql-str))
    (jdbc/query
      db (cons sql-str params)
      (select-keys q [:as-arrays? :identifiers :row-fn :result-set-fn]))
    (jdbc/execute!
      db (vec (cons sql-str params))
      (select-keys q [:multi? :transaction?]))))

