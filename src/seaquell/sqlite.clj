(ns seaquell.sqlite
  (:require [seaquell.core :refer [select! from where order-by db]]))

(defn db-spec
  ([] (db-spec ":memory:"))
  ([path]
   {:classname "org.sqlite.JDBC", :subprotocol "sqlite", :subname (name path)}))

(defn tables [conn]
  (let [xs (select! :name (from :sqlite_master) (where {:type "table"}) (order-by :name) (db conn))]
    (mapv :name xs)))

(defn schema [conn tbl]
  (let [xs (select! :sql (from :sqlite_master) (where {:type "table", :name (name tbl)}) (db conn))]
    (-> xs first :sql)))

