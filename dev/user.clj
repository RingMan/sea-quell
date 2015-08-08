(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (clear refresh refresh-all)])
  (:require [clojure.java.jdbc :as jdb])
  (:require [seaquell.core :refer :all])
  (:require [seaquell.engine :refer :all]))

(def q1 (select ['(avg Price) :as :AvgPrice '(count Price) :as :CntPrice] (from :cars)))

(def q2
  (select
    ['(sqrt (sum (/ (* (- price q.AvgPrice) (- price q.AvgPrice)) (- q.CntPrice 1)))) :as :sdev]
    (from :cars (join q1 :as :q))))

(def sq3 {:classname "org.sqlite.JDBC", :subprotocol "sqlite", :subname "/home/david/clj/sqlite/test.db"})

(defn start
  "Start the application"
  []
  )

(defn stop
  "Stop the application"
  []
  )

(defn reset []
  (stop)
  (refresh :after 'user/start))

