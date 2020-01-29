(ns user
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
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
            [zprint.config :refer [default-zprint-options get-options get-default-options
                                   reset-default-options!  reset-options!]]
            [zprint.core :as zp :refer [czprint czprint-fn set-options!]]
            [zprint.cutil :refer [kcol ks->v]]
            [zprint.rutil :refer [update-print-fn! render]]
            [diesel.core :refer [mk-map mk-map* edit] :as dsl]
            [diesel.edit :as ed]
            [seaquell.core :refer :all :rename {into s-into #_#_update s-update}]
            #_[seaquell.core :refer :all]
            [seaquell.to-sql :as sql]
            [seaquell.engine :as eng :refer :all]
            [seaquell.raw :as r]
            [seaquell.sqlite :as sq3 :refer [db-spec tables]]
            [seaquell.spec :as ss]
            [seaquell.syntax :as syn]
            [seaquell.util :as u]))

(def colors #{:black :white :red :green :blue :cyan :magenta :yellow})

(def zp-colors
  {:brace :white,
   :bracket :white,
   :char :bright-magenta,
   :comma :bright-white,
   :comment :bright-black,
   :deref :red,
   :false :bright-magenta,
   :fn :bright-red,
   :hash-brace :white ,
   :hash-paren :white,
   :keyword :blue,
   :nil :bright-magenta,
   :none :white,
   :number :bright-magenta,
   :paren :white,
   :quote :bright-yellow,
   :regex :bright-cyan,
   :string :bright-green,
   :symbol :bright-white,
   :syntax-quote :bright-yellow,
   :syntax-quote-paren :white,
   :true :bright-magenta,
   :uneval :bright-red,
   :unquote :bright-yellow,
   :unquote-splicing :bright-yellow,
   :user-fn :bright-yellow})

(def zp-opt
  {:color-map zp-colors
   :map {:comma? true
         :indent 2
         :key-order [:id :name :tag]
         :key-color (kcol (ks->v [:id :name :tag] [:bold]) zp-colors)}
   :vector {:wrap-after-multi? false}
   :uneval {:color-map zp-colors}})

(set-options! zp-opt)

(update-print-fn! render)

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

