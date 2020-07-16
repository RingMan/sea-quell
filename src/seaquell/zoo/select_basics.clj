(ns seaquell.zoo.select-basics
  (:refer-clojure :exclude [drop into update partition-by])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 4/19/2013) to the
;; tutorial at http://sqlzoo.net/wiki/SELECT_basics
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.select-basics)
  (println (select$ q1)))

(def q1 (select :population (from :bbc) (where {:name "Germany"})))

(def q2 (select [:name [/ :gdp :population]]
                (from :bbc) (where [> :area 5000000])))

(def q3 (select [:name :region]
                (from :bbc)
                (where {:area [< 2000]
                        :gdp [> 5000000000]})))

(def q4
  (select
    [:name :population] (from :bbc)
    (where [:in :name [vals "Denmark" "Finland" "Norway" "Sweden"]])))

(def q5 (select :name (from :bbc) (where [:like :name "G%"])))

(def q6 (select [:name [/ :area 1000]] (from :bbc)
                (where [:between :area 207600 244820])))

