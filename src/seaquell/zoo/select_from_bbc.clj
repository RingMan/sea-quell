(ns seaquell.zoo.select-from-bbc
  (:refer-clojure :exclude [update partition-by])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 4/19/2013) to the
;; tutorial at http://sqlzoo.net/wiki/SELECT_from_BBC_Tutorial
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.select-from-bbc)
  (println (select$ q1)))

(def q1 (select [:name :region :population] (from :bbc)))

(def q2 (select :name (from :bbc) (where [> :population 200000000])))

(def q3 (select [:name [/ :gdp :population]]
                (from :bbc)
                (where {:population [> 200000000]})))

(def q4 (select [:name [/ :population 1000000]]
                (from :bbc)
                (where {:region "Middle East"})))

(def q5 (select [:name :population]
                (from :bbc)
                (where [:in :name [vals "France" "Germany" "Italy"]])))

(def q6 (select :name (from :bbc) (where [:like :name "United%"])))

