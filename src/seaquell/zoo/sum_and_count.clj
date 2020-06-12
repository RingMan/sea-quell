(ns seaquell.zoo.sum-and-count
  (:refer-clojure :exclude [into update partition-by])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 4/19/2013) to the
;; tutorial at http://sqlzoo.net/wiki/SUM_and_COUNT
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.sum-and-count)
  (println (select$ q1)))

(def q1 (select [[:sum :population]] (from :bbc)))

(def q2 (select-distinct :region (from :bbc)))

(def q3
  (select [[:sum :gdp]] (from :bbc) (where {:region "Africa"})))

(def q4
  (select [[count :name]] (from :bbc) (where [>= :area 1000000])))

(def q5
  (select [[:sum :population]]
          (from :bbc)
          (where [:in :name [vals "France" "Germany" "Spain"]])))

(def q6
  (select [:region [count :name]] (from :bbc) (group :region)))

(def q7
  (select [:region [count :name]]
          (from :bbc)
          (where [>= :population 10000000])
          (group :region)))

(def q8
  (select :region (from :bbc) (group :region)
          (having [>= [:sum :population] 100000000])))
