(ns user
  (:refer-clojure :rename {update clj-update,
                           partition-by clj-partition-by})
  (:require [clojure.core :as c]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [midje.repl :refer [autotest]]
            [clojure.java.jdbc :as jdb]
            [diesel.core :refer [mk-map mk-map* edit] :as dsl]
            [diesel.edit :as ed]
            #_[seaquell.core :refer :all :rename {update sql-update}]
            [seaquell.core :refer :all]
            [seaquell.to-sql :as sql]
            [seaquell.engine :as eng :refer :all]
            [seaquell.util :as u]))

(def q1 (select ['(avg Price) :as :AvgPrice '(count Price) :as :CntPrice] (from :cars)))

(def q2
  (select
    ['(sqrt (sum (/ (* (- price q.AvgPrice) (- price q.AvgPrice)) (- q.CntPrice 1)))) :as :sdev]
    (from :cars (join q1 :as :q))))

#_(def sq3 {:classname "org.sqlite.JDBC", :subprotocol "sqlite", :subname "/home/david/clj/sqlite/test.db"})
(def sq3 {:classname "org.sqlite.JDBC", :subprotocol "sqlite", :subname ":memory:"})

(integrant.repl/set-prep! (constantly {}))

