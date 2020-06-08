(ns seaquell.engine
  (:require [clojure.java.jdbc :as jdbc]
            [diesel.core :refer [def-props def-vec-props def-bool-props]]
            [seaquell.util :as u]))

(defn db-conn [db-spec]
  (->> db-spec jdbc/get-connection (jdbc/add-connection db-spec)))

;; Syntax for jdbc pass-thru options

(def-bool-props as-arrays? keywordize? transaction? multi?)

(def-props identifiers qualifier row-fn result-set-fn)

(defn id-fn [f] {:identifiers f})

;; Query execution using jdbc

(defn exec
  "Executes SQL CRUD operations using clojure.java.jdbc
  Takes a map with two mandatory keys:
    :db - DB spec or connection pool datasource
    :sql-str - SQL string to execute
  And two optional keys:
    :params - Parameters to pass to jdbc
    :jdbc/query? - Force exec to call jdbc/query if truthy. Useful for literal
                   SELECT statements introduced by a WITH clause.
  The following are passed directly to jdbc/query or jdbc/execute!, respectively:
    :as-arrays? :identifiers :keywordize? :qualifier :row-fn :result-set-fn
    :multi? :transaction?"
  [{:keys [db sql-str params] :as q}]
  {:pre [(and db sql-str)]}
  (if (or (u/select? q) (u/compound-select? q) (:jdbc/query? q)
          (re-find #"^(?i)(select|values|explain) " sql-str)
          (re-find #"^(?i)\s*ALTER\s+TABLE\s+\S+\s+RENAME\W" sql-str)
          (re-find #"^(?i)\s*PRAGMA\s+(?:\w|\.)+\s*;" sql-str))
    (jdbc/query
      db (cons sql-str params)
      (select-keys q [:as-arrays? :identifiers :keywordize? :qualifier :row-fn :result-set-fn]))
    (jdbc/execute!
      db (vec (cons sql-str params))
      (select-keys q [:multi? :transaction?]))))

