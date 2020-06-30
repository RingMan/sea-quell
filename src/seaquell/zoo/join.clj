(ns seaquell.zoo.join
  (:refer-clojure
    :exclude [distinct drop group-by into set update partition-by when])
  (:require [seaquell.core :refer :all]))

;; The following queries are valid solutions (as of 3/31/2013) to the
;; tutorial at http://sqlzoo.net/wiki/The_JOIN_operation
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.join)
  (println (select$ q1)))

(def q1
  (select
    [:matchid :player]
    (from :goal)
    (where {:teamid "GER"})))

(def q2
  (select
    [:id :stadium :team1 :team2]
    (from :game)
    (where {:id 1012})))

(def q3
  (select [:player :teamid :mdate]
          (from :game (join :goal (on {:id :matchid})))
          (where {:teamid "GER"})))

(def q4
  (select [:team1 :team2 :player]
          (from :game (join :goal (on {:id :matchid})))
          (where [:like :player "mario%"])))


(def q5
  (select [:player :teamid :coach :gtime]
          (from :goal (join :eteam :on {:teamid :id}))
          (where [<= :gtime 10])))

(def q6
  (select [:mdate :teamname]
          (from :game (join :eteam :on {:team1 :eteam.id}))
          (where {:coach "Fernando Santos"})))

(def q7
  (select [:player]
          (from :goal (join :game (on {:id :matchid})))
          (where {:stadium "National Stadium, Warsaw"})))

(def q8
  (select-distinct [:player]
                   (from :game (join :goal (on {:id :matchid})))
                   (where '(and (or (= team1 "GER") (= team2 "GER"))
                                (not= teamid "GER")))))

(def q9
  (select [:teamname [count :gtime]]
          (from :eteam (join :goal :on {:teamid :id}))
          (group-by :teamname)))

(def q10
  (select [:stadium [count :gtime]]
          (from :goal (join :game (on {:id :matchid})))
          (group-by :stadium)))

(def q11
  (select [:matchid :mdate [count :gtime]]
          (from :game (join :goal (on {:id :matchid})))
          (where '(or (= team1 "POL") (= team2 "POL")))
          (group-by :matchid :mdate)))

(def q12
  (select [:matchid :mdate [count :gtime]]
          (from :game (join :goal (on {:id :matchid})))
          (where {:teamid "GER"})
          (group-by :matchid :mdate)))

(def q13
  (select
    '[mdate
      team1, (sum (cond (= teamid team1) 1 :else 0)) :as score1
      team2, (sum (cond (= teamid team2) 1 :else 0)) :as score2]
    (from :game (join :goal :on {:matchid :id}))
    (group-by :mdate :matchid :team1 :team2)))
