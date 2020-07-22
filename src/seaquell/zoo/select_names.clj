(ns seaquell.zoo.select-names
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 7/21/2020) to the
;; tutorial at http://sqlzoo.net/wiki/SELECT_names
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.select-names)
  (select$ q1))

(def q (select :name (from :world)))

(def q1 (select q (where {:name [:like "Y%"]})))

(def q2 (select q (where {:name [:like "%y"]})))

(def q3 (select q (where {:name [:like "%x%"]})))

(def q4 (select q (where {:name [:like "%land"]})))

(def q5 (select q (where {:name [:like "C%ia"]})))

(def q6 (select q (where {:name [:like "%oo%"]})))

(def q7 (select q (where {:name [:like "%a%a%a%"]})))

(def q8 (select q (where {:name [:like "_t%"]}) (order-by :name)))

(def q9 (select q (where {:name [:like "%o__o%"]})))

(def q10 (select q (where {:name [:like "____"]})))

(def q11 (select q (where {:name :capital})))

(def q12 (select q (where {:capital [:concat :name " City"]})))

(def q13 (select [:capital :name] (from :world)
                 (where {:capital [:like [:concat "%" :name "%"]]})))

(def q14 (select [:capital :name] (from :world)
                 (where {:capital [:like [:concat :name "_%"]]})))

(def q15 (select [:name '(mid capital (+ (length name) 1)) (as :ext)] (from :world)
                 (where {:capital [:like [:concat :name "_%"]]})))

