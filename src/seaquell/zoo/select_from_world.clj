(ns seaquell.zoo.select-from-world
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 7/21/2020) to the
;; tutorial at http://sqlzoo.net/wiki/SELECT_from_WORLD_Tutorial
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.select-from-world)
  (select$ q1))

(def q1 (select [:name :continent :population] (from :world)))

(def q2 (select :name (from :world) (where [> :population 200000000])))

(def q3 (select [:name [/ :gdp :population]]
                (from :world)
                (where {:population [> 200000000]})))

(def q4 (select [:name [/ :population 1000000]]
                (from :world)
                (where {:continent "South America"})))

(def q5 (select [:name :population]
                (from :world)
                (where [:in :name [vals "France" "Germany" "Italy"]])))

(def q6 (select :name (from :world) (where [:like :name "%United%"])))

(def q7 (select [:name :population :area] (from :world)
                (where [:or [> :area 3000000] [> :population 250000000]])))

(def q8
  (select
    [:name :population :area] (from :world)
    (where '(and (or (> area 3000000) (> population 250000000))
                 (not (and (> area 3000000) (> population 250000000)))))))

(def q8-b (select
          [:name :population :area] (from :world)
          (where '(xor (> area 3000000) (> population 250000000)))))

(def q9
  (select
    [:name [:round [/ :population 1000000] 2] [:round [/ :gdp 1000000000] 2]]
    (from :world)
    (where {:continent "South America"})))

(def q10 (select [:name [:round [/ :gdp :population] -3]] (from :world)
                 (where {:gdp [> 1000000000000]})))

(def q11 (select [:name :capital] (from :world)
                 (where {[:length :name] [:length :capital]})))

(def q12 (select [:name :capital] (from :world)
                 (where {[:left :name 1] [:left :capital 1]
                         :name [not= :capital]})))

(def q13 (select :name (from :world)
                 (where '(and [:like :name "%a%"]
                              [:like :name "%e%"]
                              [:like :name "%i%"]
                              [:like :name "%o%"]
                              [:like :name "%u%"]
                              [:not-like :name "% %"]))))

