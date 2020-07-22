(ns seaquell.zoo.sum-and-count
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 7/21/2020) to the
;; tutorial at http://sqlzoo.net/wiki/SUM_and_COUNT
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.sum-and-count)
  (select$ q1))

(def q1 (select [[:sum :population]] (from :world)))

(def q2 (select-distinct :continent (from :world)))

(def q3
  (select [[:sum :gdp]] (from :world) (where {:continent "Africa"})))

(def q4
  (select [[count :name]] (from :world) (where [>= :area 1000000])))

(def q5
  (select [[:sum :population]]
          (from :world)
          (where [:in :name [vals "Estonia" "Latvia" "Lithuania"]])))

(def q6
  (select [:continent [count :name]] (from :world) (group-by :continent)))

(def q7
  (select [:continent [count :name]]
          (from :world)
          (where [>= :population 10000000])
          (group-by :continent)))

(def q8
  (select :continent (from :world) (group-by :continent)
          (having [>= [:sum :population] 100000000])))
