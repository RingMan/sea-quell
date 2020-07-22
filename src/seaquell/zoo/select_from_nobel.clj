(ns seaquell.zoo.select-from-nobel
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 7/21/2020) to the
;; tutorial at http://sqlzoo.net/wiki/SELECT_from_Nobel_Tutorial
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.select-from-nobel)
  (select$ q1))

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
                                   "Jimmy Carter"
                                   "Barack Obama")))))

(def q7 (select :winner :from :nobel :where [:like :winner "John %"]))

(def q8
  (select
    :* (from :nobel)
    (where '(or {subject "Physics", yr 1980}
                {subject "Chemistry", yr 1984}))))

(def q9
  (select
    :* (from :nobel)
    (where {:yr 1980, :subject [:not-in [vals "Chemistry" "Medicine"]]})))

(def q10
  (select
    :* (from :nobel)
    (where '(or {subject "Medicine", yr [< 1910]}
                {subject "Literature", yr [>= 2004]}))))

(def q11
  "If the umlaut doesn't print properly at the REPL, you may need to change
  the encoding to match the UTF-8 encoding of this file. On a Windows 10
  machine, changing the code page by entering `chcp 65001` before starting
  the REPL does the trick."
  (select
    :* (from :nobel)
    (where {:winner [:in [val "Peter Grünberg"]]})))

(def q12
  (select
    :* (from :nobel)
    (where {:winner [:in [val "Eugene O''Neill"]]})))

(def q13
  (select
    [:winner :yr :subject] (from :nobel)
    (where {:winner [:like "Sir%"]})
    (order-by (desc :yr) :winner)))

(def q14
  (select
    [:winner :subject] (from :nobel)
    (where {:yr 1984})
    (order-by [:in :subject [vals "Physics" "Chemistry"]] :subject :winner)))

