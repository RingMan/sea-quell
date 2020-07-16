(ns seaquell.zoo.window-fns
  (:refer-clojure :exclude [drop into update partition-by])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 2/18/2020) to the
;; tutorial at http://sqlzoo.net/wiki/Window_functions
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.window-fns)
  (println (select$ q1)))

(def q1
  (select [:lastName :party :votes]
          (from :ge)
          (where {:constituency "S14000024", :yr 2017})
          (order-by (desc :votes))))

(def q2
  (select
    [:party :votes (field [:RANK] (over (order-by (desc :votes))) (as :posn))]
    (from :ge)
    (where {:constituency "S14000024", :yr 2017})
    (order-by :party)))

(def q3
  (select
    [:yr :party :votes
     (field [:RANK] (over (partition-by :yr) (order-by (desc :votes))) (as :posn))]
    (from :ge)
    (where {:constituency "S14000021"})
    (order-by :party :yr)))

(def q4
  (select
    [:constituency :party :votes
     (field [:RANK] (over (partition-by :constituency) (order-by (desc :votes))))
     (as :posn)]
    (from :ge)
    (where {:constituency [:between "S14000021" "S14000026"], :yr 2017})
    (order-by :posn :constituency)))

(def q5
  (select
    [:constituency :party]
    (from
      (select
        [:constituency :party
         (field [:RANK] (over (partition-by :constituency) (order-by (desc :votes))))
         (as :posn)]
        (from :ge)
        (where {:constituency [:between "S14000021" "S14000026"], :yr 2017}))
      (as :ed))
    (where {:posn 1})))

(def q6
  (select
    [:party [count 1]]
    (from
      (select
        [:constituency :party
         (field [:RANK] (over (partition-by :constituency) (order-by (desc :votes))))
         (as :posn)]
        (from :ge)
        (where {:constituency [:like "S%"], :yr 2017}))
      (as :ed))
    (where {:posn 1})
    (group :party)))

