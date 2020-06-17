(ns seaquell.zoo.select-from-nobel
  (:refer-clojure :exclude [distinct drop group-by into update partition-by])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 4/19/2013) to the
;; tutorial at http://sqlzoo.net/wiki/SELECT_from_Nobel_Tutorial
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.select-from-nobel)
  (println (select$ q1)))

(def q1 (select [:yr :subject :winner] :from :nobel :where {:yr 1950}))

(def q2
  (select :winner :from :nobel :where {:yr 1962, :subject "Literature"}))

(def q3
  (select [:yr :subject] :from :nobel :where {:winner "Albert Einstein"}))

(def q4
  (select :winner :from :nobel :where {:subject "Peace", :yr [>= 2000]}))

(def q5
  (select [:yr :subject :winner]
          (from :nobel)
          (where {:subject "Literature", :yr [:between 1980 1989]})))

(def q6
  (select :* (from :nobel)
          (where '(in winner (vals "Theodore Roosevelt"
                                   "Woodrow Wilson"
                                   "Jed Bartlet"
                                   "Jimmy Carter")))))

(def q7 (select :winner :from :nobel :where [:like :winner "John %"]))

(def q8
  (select-distinct
    :yr (from :nobel)
    (where {:subject "Physics"
            :yr [:not-in (select :yr :from :nobel
                                 (where {:subject "Chemistry"}))]})))

(def q8-b
  (select-distinct
    :yr
    (from (select [:yr :subject] (from :nobel)
                  (where {:subject "Physics"})) (as :n1)
          (left-join (select [:yr :subject] (from :nobel)
                        (where {:subject "Chemistry"})) (as :n2)
                     (using :yr)))
    (where '(is n2.subject nil))))
