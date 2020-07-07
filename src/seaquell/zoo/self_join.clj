(ns seaquell.zoo.self-join
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
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
                (group-by :company :num)
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
  (select
    [:ra.num :ra.company :sb.name :rd.num :rd.company]
    (from :route (as :ra)
          (join :stops (as :sa) (on {:ra.stop :sa.id}))
          (join :route (as :rb) (using :num :company))
          (join :stops (as :sb) (on {:rb.stop :sb.id}))
          (join :route (as :rc))
          (join :stops (as :sc) (on {:rc.stop :sc.id}))
          (join :route (as :rd) (on {:rc.num :rd.num, :rc.company :rd.company}))
          (join :stops (as :sd) (on {:rd.stop :sd.id})))
    (where {:sa.name "Craiglockhart", :sd.name "Lochend", :sb.name :sc.name})
    (order-by :ra.company :ra.num :sb.name :rd.company :rd.num)))

(def q10-b
  (select
    [:q1.num :q1.company :q1.name :q2.num :q2.company]
    (from
      (select
        [:x.num :x.company :b.name]
        (from :route (as :x)
              (join :stops (as :a) (on {:x.stop :a.id}))
              (join :route (as :y) (on {:x.num :y.num, :x.company :y.company}))
              (join :stops (as :b) (on {:y.stop :b.id})))
        (where {:a.name "Craiglockhart"}))
      (as :q1)
      (join
        (select
          [:x.num :x.company :a.name]
          (from :route (as :x)
                (join :stops (as :a) (on {:x.stop :a.id}))
                (join :route (as :y) (on {:x.num :y.num, :x.company :y.company}))
                (join :stops (as :b) (on {:y.stop :b.id})))
          (where {:b.name "Lochend"}))
        (as :q2)
        (on {:q1.name :q2.name})))
    (order-by :q1.company :q1.num :q1.name :q2.company :q2.num)))

(def q10-c
  (with
    :dest[:company :num :pos :name] :as
    (select [:company :num :pos :name]
            (from :route (join :stops (on {:stop :id})))),
    :leg[:company :num :p1 :s1 :p2 :s2] :as
    (select
      [:a.company :a.num :a.pos :a.name :b.pos :b.name]
      (from :dest (as :a)
            (join :dest (as :b) (on {:a.company :b.company
                                     :a.num :b.num
                                     :a.pos [not= :b.pos]})))),
    :leg1[:company :num :p1 :s1 :p2 :s2] :as
    (select :* (from :leg) (where {:s1 "Craiglockhart"}))
    :leg2[:company :num :p1 :s1 :p2 :s2] :as
    (select :* (from :leg) (where {:s2 "Lochend"}))
    (select
      [:leg1.num :leg1.company :leg1.s2 (as :name) :leg2.num :leg2.company]
      (from :leg1 (join :leg2 (on {:leg1.s2 :leg2.s1})))
      (order-by :leg1.company :leg1.num :name :leg2.company :leg2.num))))

