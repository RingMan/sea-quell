(ns seaquell.zoo.select-within-select
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are valid solutions (as of 7/21/2020) to the
;; tutorial at http://sqlzoo.net/wiki/SELECT_within_SELECT_Tutorial
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.select-within-select)
  (select$ q1))

(def q1
  (select
    :name
    (from :world)
    (where [> :population
              (select
                :population
                (from :world)
                (where {:name "Russia"}))])))

(def q2
  (select
    :name
    (from :world)
    (where {:continent "Europe"
            [/ :gdp :population]
            [> (select [[/ :gdp :population]]
                       (from :world)
                       (where {:name "United Kingdom"}))]})))

(def q3
  (select
    [:name :continent]
    (from :world)
    (where [:in :continent
            (select :continent
                    (from :world)
                    (where [:in :name [vals "Australia" "Argentina"]]))])
    (order-by :name)))

(def pop-canada (select [[+ :population 1]] :from :world :where {:name "Canada"}))
(def pop-poland (select [[- :population 1]] :from :world :where {:name "Poland"}))

(def q4 (select [:name :population] (from :world)
                (where {:population [:between pop-canada pop-poland]})))

(def q5
  (let [pop-germany (select :population (from :world)
                            (where {:name "Germany"}))]
    (select
      [:name
       `(concat (cast (round (* 100 (/ population ~pop-germany)) 0) int) "%")]
      (from :world)
      (where {:continent "Europe"}))))

(def gdp-of-europe
  (select :gdp :from :world :where {:continent "Europe" :gdp [:is-not nil]}))

(def q6
  (select :name (from :world) (where [> :gdp [:all gdp-of-europe]])))

(def q7
  (select
    [:continent :name :area]
    (from :world :as :x)
    (where [>= :area
            [:all (select :area (from :world :as :y)
                          (where {:y.continent :x.continent
                                  :area [> 0]}))]])))

(def q8
  (select [:continent :name] (from :world (as :x))
          (where {:x.name [<= [:all (select
                                      :name (from :world (as :y))
                                      (where {:x.continent :y.continent}))]]})))

(def q9
  (select
    [:name :continent :population]
    (from :world (as :x))
    (where [> 25000000
            [:all (select
                    :population (from :world :as :y)
                    (where {:x.continent :y.continent
                            :y.population [> 0]}))]])))

(def q10
  (select
    [:name :continent]
    (from :world :as :w1)
    (where [> :population
            [:all (select [[* :population 3]]
                          (from :world :as :w2)
                          (where {:w1.continent :w2.continent
                                  :w1.name [not= :w2.name]}))]])))
