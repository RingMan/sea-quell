(ns seaquell.zoo.self-join
  (:refer-clojure :exclude [update partition-by])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 4/20/2013) to the
;; tutorial at http://sqlzoo.net/wiki/Self_join
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.self-join)
  (println (select$ q1)))

(def q1 (select [[count :*]] (from :stops)))

(def q2 (select :id (from :stops) (where {:name "Craiglockhart"})))

(def q3
  (select [:id :name]
          (from :stops :route)
          (where {:id :stop, :company "LRT", :num "4"})))

(def q3-b
  (select [:id :name]
          (from :route (join :stops (on {:id :stop})))
          (where {:company "LRT", :num "4"})))

(def q4 (select [:company :num [count :*]]
                (from :route)
                (where '(or [= stop 149] [= stop 53]))
                (group :company :num)
                (having [= [count :*] 2])))

(def q5 (select [:a.company :a.num :a.stop :b.stop]
                (from :route (as :a)
                      (join :route (as :b) (on {:a.company :b.company
                                                :a.num :b.num })))
                (where {:a.stop 53, :b.stop 149})))

(def q6
  (select
    [:a.company :a.num :stopa.name :stopb.name]
    (from :route (as :a)
          (join :route (as :b) (on {:a.company :b.company :a.num :b.num }))
          (join :stops (as :stopa) (on {:a.stop :stopa.id}))
          (join :stops (as :stopb) (on {:b.stop :stopb.id})))
    (where {:stopa.name "Craiglockhart", :stopb.name "London Road"})))

(def q7
  (select-distinct
    [:R1.company :R1.num]
    (from :route (as :R1)
          (comma-join :route (as :R2)))
    (where {:R1.num :R2.num
            :R1.company :R2.company
            :R1.stop 115
            :R2.stop 137})))

(def q7-b
  (select-distinct
    [:R1.company :R1.num]
    (from :route (as :R1)
          (join :route (as :R2) (on {:R1.num :R2.num
                                     :R1.company :R2.company })))
    (where {:R1.stop 115
            :R2.stop 137})))

(def q8
  (select [:R1.company :R1.num]
          (from :route :as :R1
                (comma-join :route :as :R2)
                (comma-join :stops :as :S1)
                (comma-join :stops :as :S2))
          (where {:R1.num :R2.num, :R1.company :R2.company
                  :R1.stop :S1.id, :R2.stop :S2.id
                  :S1.name "Craiglockhart"
                  :S2.name "Tollcross"})))

(def q8-b
  (select [:R1.company :R1.num]
          (from :route :as :R1
                (join :route :as :R2 (on {:R1.num :R2.num
                                          :R1.company :R2.company}))
                (join :stops :as :S1 (on {:R1.stop :S1.id}))
                (join :stops :as :S2 (on {:R2.stop :S2.id})))
          (where {:S1.name "Craiglockhart"
                  :S2.name "Tollcross"})))

(def q9
  (select [:S2.name :R2.company :R2.num]
          (from :stops :as :S1
                (comma-join :stops :as :S2)
                (comma-join :route :as :R1)
                (comma-join :route :as :R2))
          (where {:S1.name "Craiglockhart"
                  :S1.id :R1.stop
                  :R1.company :R2.company, :R1.num :R2.num
                  :R2.stop :S2.id})))

(def q9-b
  (select [:S2.name :R2.company :R2.num]
          (from :route :as :R1
                (join :route :as :R2 (on {:R1.company :R2.company
                                          :R1.num :R2.num }))
                (join :stops :as :S1 (on {:S1.id :R1.stop }))
                (join :stops :as :S2 (on {:S2.id :R2.stop})))
          (where {:S1.name "Craiglockhart"})))

(def q10
  (select-distinct
    [:a.name :c.name]
    (from :stops (as :a)
          (join :route (as :z) (on {:a.id :z.stop}))
          (join :route (as :y) (on {:y.num :z.num}))
          (join :stops (as :b) (on {:y.stop :b.id}))
          (join :route (as :x) (on {:x.num :y.num}))
          (join :stops (as :c) (on {:c.id :x.stop})))
    (where {:a.name "Craiglockhart", :c.name "Sighthill"})))

