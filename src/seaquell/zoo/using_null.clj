(ns seaquell.zoo.using-null
  (:refer-clojure :exclude [distinct drop group-by into set update partition-by])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 4/20/2013) to the
;; tutorial at http://sqlzoo.net/wiki/Using_Null
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.using-null)
  (println (select$ q1)))

(def q1 (select :name (from :teacher) (where [:is :dept nil])))

(def q2
  (select [:teacher.name :dept.name]
          (from :teacher
                (inner-join :dept (on {:teacher.dept :dept.id})))))

(def q3
  (select [:teacher.name :dept.name]
          (from :teacher
                (left-join :dept (on {:teacher.dept :dept.id})))))

(def q4
  (select [:teacher.name :dept.name]
          (from :teacher
                (right-join :dept (on {:teacher.dept :dept.id})))))

(def q5
  (select [:name [:coalesce :mobile "07986 444 2266"]] (from :teacher)))

(def q6
  (select [:teacher.name '(coalesce dept.name "None")]
          (from :teacher
                (left-join :dept (on {:teacher.dept :dept.id})))))

(def q7 (select [[count :teacher.name] [count :mobile]] (from :teacher)))

(def q8
  (select [:dept.name [count :teacher.name]]
          (from :teacher
                (right-join :dept (on {:teacher.dept :dept.id})))
          (group-by :dept.name)))

(def q9
  (select [:name '(cond
                    (in dept [vals 1 2]) "Sci"
                    :else "Art")]
          (from :teacher)))

(def q10
  (select [:name '(cond
                    (in dept [vals 1 2]) "Sci"
                    (= dept 3) "Art"
                    :else "None")]
          (from :teacher)))

(def q10-b
  (select [:name '(cond
                    (or (= dept 1) (= dept 2)) "Sci"
                    (= dept 3) "Art"
                    :else "None")]
          (from :teacher)))
