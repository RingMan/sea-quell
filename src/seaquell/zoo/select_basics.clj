(ns seaquell.zoo.select-basics
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 7/21/2020) to the
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

(def q1 (select :population (from :world) (where {:name "Germany"})))

(def q2
  (select
    [:name :population] (from :world)
    (where [:in :name [vals "Denmark" "Norway" "Sweden"]])))

(def q3 (select [:name :area] (from :world)
                (where [:between :area 200000 250000])))

