(ns seaquell.zoo.select-within-select
  (:use seaquell.core))

;; The following queries are valid solutions (as of 3/31/2013) to the
;; tutorial at http://sqlzoo.net/wiki/SELECT_within_SELECT_Tutorial
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.select-within-select)
  (println (select$ q1)))

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
    [:name :continent]
    (from :world)
    (where [:in :continent
                (select :continent
                        (from :world)
                        (where [:in :name [vals "belize" "belgium"]]))])))

(def q3
  (select
    :name
    (from :world)
    (where {:continent "europe"
            [/ :gdp :population]
            [> (select [[/ :gdp :population]]
                       (from :world)
                       (where {:name "united kingdom"}))]})))

(def pop-canada (select :population :from :world :where {:name "canada"}))
(def pop-poland (select :population :from :world :where {:name "poland"}))
(def q4 (select [:name :population] (from :world)
                (where [:and [> :population pop-canada]
                             [< :population pop-poland]])))

(def gdp-of-europe (select :gdp :from :world :where {:continent "europe"}))
(def q5
  (select :name (from :world) (where [> :gdp [:all gdp-of-europe]])))

(def q6
  (select
    [:continent :name :area]
    (from :world :as :x)
    (where [>= :area
               [:all (select :area (from :world :as :y)
                             (where {:y.continent :x.continent
                                     :area [> 0]}))]])))

(def q7
  (select
    [:name :continent :population]
    (from :world)
    (where [:in :continent
                (select-distinct
                  :continent (from :world :as :w1)
                  (where
                    [> 25000000
                       [:all (select
                               :population (from :world :as :w2)
                               (where {:w1.continent :w2.continent}))]]))])))

(def q8
  (select
    [:name :continent]
    (from :world :as :w1)
    (where [> :population
              [:all (select [[* :population 3]]
                            (from :world :as :w2)
                            (where {:w1.continent :w2.continent
                                    :w1.name [not= :w2.name]}))]])))
