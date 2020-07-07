(ns seaquell.zoo.select-within-select
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

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
    (from :bbc)
    (where [> :population
              (select
                :population
                (from :bbc)
                (where {:name "Russia"}))])))

(def q2
  (select
    [:name :region]
    (from :bbc)
    (where [:in :region
                (select :region
                        (from :bbc)
                        (where [:in :name [vals "India" "Iran"]]))])))

(def q3
  (select
    :name
    (from :bbc)
    (where {:region "europe"
            [/ :gdp :population]
            [> (select [[/ :gdp :population]]
                       (from :bbc)
                       (where {:name "united kingdom"}))]})))

(def pop-canada (select :population :from :bbc :where {:name "canada"}))
(def pop-algeria (select :population :from :bbc :where {:name "algeria"}))
(def q4 (select :name (from :bbc)
                (where [:and [> :population pop-canada]
                             [< :population pop-algeria]])))

(def gdp-of-europe
  (select :gdp :from :bbc :where {:region "europe" :gdp [:is-not nil]}))

(def q5
  (select :name (from :bbc) (where [> :gdp [:all gdp-of-europe]])))

(def q6
  (select
    [:region :name :population]
    (from :bbc :as :x)
    (where [>= :population
               [:all (select :population (from :bbc :as :y)
                             (where {:y.region :x.region
                                     :population [> 0]}))]])))

(def q7
  (select
    [:name :region :population]
    (from :bbc (as :x))
    (where [> 25000000
            [:all (select
                    :population (from :bbc :as :y)
                    (where {:x.region :y.region
                            :y.population [> 0]}))]])))

(def q8
  (select
    [:name :region]
    (from :bbc :as :w1)
    (where [> :population
              [:all (select [[* :population 3]]
                            (from :bbc :as :w2)
                            (where {:w1.region :w2.region
                                    :w1.name [not= :w2.name]}))]])))
