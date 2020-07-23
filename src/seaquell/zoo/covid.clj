(ns seaquell.zoo.covid
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 7/21/2020) to the
;; tutorial at https://sqlzoo.net/wiki/Window_LAG
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.covid)
  (println (select$ q1)))

(def q1
  (select [:name [:DAY :whn] :confirmed :deaths :recovered]
          (from :covid)
          (where {:name "Spain", [:month :whn] 3})
          (order-by :whn)))

(def q2
  (select
    [:name [:day :whn] :confirmed
     (field [:lag :confirmed 1] (over (partition-by :name) (order-by :whn))
            (as :lag))]
    (from :covid)
    (where {:name "Italy", [:month :whn] 3})
    (order-by :whn)))

(def q3
  (select
    [:name [:day :whn]
     (field [- :confirmed [:lag :confirmed 1]]
            (over (partition-by :name) (order-by :whn))
            (as :cases))]
    (from :covid)
    (where {:name "Italy", [:month :whn] 3})
    (order-by :whn)))

(def q4
  (select
    [:name [:date_format :whn "%Y-%m-%d"]
     (field [- :confirmed [:lag :confirmed 1]]
            (over (partition-by :name) (order-by :whn))
            (as :cases))]
    (from :covid)
    (where {:name "Italy", [:weekday :whn] 0})
    (order-by :whn)))

(def q5
  (select
    [:tw.name [:date_format :tw.whn "%Y-%m-%d"] [- :tw.confirmed :lw.confirmed]]
    (from :covid (as :tw)
          (left-join :covid (as :lw)
                     (on {[:date_add :lw.whn (interval 1 :week)] :tw.whn
                          :tw.name :lw.name})))
    (where {:tw.name "Italy", [:weekday :tw.whn] 0})
    (order-by :tw.whn)))

(def q6
  (select 
    [:name
     :confirmed (field [:rank] (over (order-by (desc :confirmed))) (as :rc))
     :deaths (field [:rank] (over (order-by (desc :deaths))) (as :rd))]
    (from :covid (join :world (using :name)))
    (where {:whn "2020-04-20"})
    (order-by (desc :confirmed))))

(def q7
  (select 
    [:world.name,
     [:round [* 100000 [/ :confirmed :population]] 0] (as :rate),
     (field [:rank] (over (order-by [/ :confirmed :population]))) (as :ri)]
    (from :covid (join :world (using :name)))
    (where {:whn "2020-04-20", :population [> 10000000]})
    (order-by (desc :population))))

(def q8
  "As of 7/21/2020 this query gives the correct answer sometimes. There seems
  to be some kind of ordering issue when multiple countries peak on the same
  day. Their sort order appears arbitrary. Sometimes the order just happens
  to align with the expected answer."
  (select
    [:name [:date_format :whn "%Y-%m-%d"] :newCases (as :peakNewCases)]
    (from
      (select
        [:name :whn :newCases
         (field [:rank] (over (partition-by :name) (order-by (desc :newCases)))
                (as :rnc))]
        (from
          (select [:name, :whn, (field [- :confirmed [:lag :confirmed 1]]
                                       (over (partition-by :name) (order-by :whn))
                                       (as :newCases))]
                  (from :covid))
          (as :x)))
      (as :y))
    (where {:rnc 1, :newCases [> 1000]})
    (order-by :whn)))

