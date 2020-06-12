(ns user
  (:refer-clojure :rename {update clj-update,
                           partition-by clj-partition-by})
  (:require [clojure.core :as c]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [expound.alpha :refer [expound]]
            [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [me.raynes.fs :as fs]
            [midje.repl :refer [autotest]]
            [clojure.java.jdbc :as jdb]
            [diesel.core :refer [mk-map mk-map* edit] :as dsl]
            [diesel.edit :as ed]
            [seaquell.core :refer :all :rename {into s-into #_#_update s-update}]
            #_[seaquell.core :refer :all]
            [seaquell.to-sql :as sql]
            [seaquell.engine :as eng :refer :all]
            [seaquell.raw :as r]
            [seaquell.sqlite :as sq3 :refer [db-spec tables]]
            [seaquell.spec :as ss]
            [seaquell.util :as u]))

(def q1 (select ['(avg Price) :as :AvgPrice '(count Price) :as :CntPrice] (from :cars)))

(def q2
  (select
    ['(sqrt (sum (/ (* (- price q.AvgPrice) (- price q.AvgPrice)) (- q.CntPrice 1)))) :as :sdev]
    (from :cars (join q1 :as :q))))

(def c (db-conn (db-spec)))

(defn sql-$ [sql-fn & args]
  (let [q (apply sql-fn args)] [q (sql$ q)]))

(defn qry! [& body]
  (apply sql! (concat body {:jdbc/query? true})))

(defn query?
  ([] {:jdbc/query? true})
  ([x] {:jdbc/query? x}))

(integrant.repl/set-prep! (constantly {}))

