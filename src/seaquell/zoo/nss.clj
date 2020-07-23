(ns seaquell.zoo.nss
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 7/21/2020) to the
;; tutorial at http://sqlzoo.net/wiki/NSS_Tutorial
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.using-null)
  (select$ q1))

(def q1
  (select :A_STRONGLY_AGREE (from :nss)
          (where {:question "Q01"
                  :institution "Edinburgh Napier University"
                  :subject "(8) Computer Science"})))

(def q2
  (select [:institution :subject] (from :nss)
          (where {:question "Q15"
                  :score [>= 100]})))

(def q3
  (select [:institution :score] (from :nss)
          (where {:question "Q15"
                  :score [< 50]
                  :subject "(8) Computer Science"})))

(def q4
  (select [:subject [:sum :response]] (from :nss)
          (where {:question "Q22"
                  :subject [:in '(vals "(8) Computer Science"
                                       "(H) Creative Arts and Design")]})
          (group-by :subject)))

(def q5
  (select [:subject '(sum (* response (/ A_STRONGLY_AGREE 100)))] (from :nss)
          (where {:question "Q22"
                  :subject [:in '(vals "(8) Computer Science"
                                       "(H) Creative Arts and Design")]})
          (group-by :subject)))

(def q6
  (select
    [:subject
     '(round (/ (sum (* response A_STRONGLY_AGREE)) (sum response)) 0)]
    (from :nss)
    (where {:question "Q22"
            :subject [:in '(vals "(8) Computer Science"
                                 "(H) Creative Arts and Design")]})
    (group-by :subject)))

(def q7
  (select
    [:institution
     '(round (/ (sum (* response score)) (sum response)) 0) (as :score)]
    (from :nss)
    (where {:question "Q22"
            :institution [:like "%Manchester%"]})
    (group-by :institution)))

(def q8
  (select
    [:institution
     [:sum :sample]
     '(sum (cond (like subject "(8)%") sample)) (as :comp)]
    (from :nss)
    (where {:question "Q01"
            :institution [:like "%Manchester%"]})
    (group-by :institution)))

