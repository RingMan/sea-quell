(ns seaquell.zoo.more-joins
  (:refer-clojure :exclude [update])
  (:require [seaquell.core :refer :all]))

;; The following queries are solutions (as of 4/20/2013) to the
;; tutorial at http://sqlzoo.net/wiki/More_JOIN_operations
;; Each query is named for the question it answers.
;;
;; To try them out, open a repl and issue the commands below.
;; Submitting the resulting SQL into the tutorial should produce
;; correct answers.

(comment
  (use 'seaquell.core)
  (use 'seaquell.zoo.more-joins)
  (println (select$ q1)))

(def q1 (select [:id :title] (from :movie) (where {:yr 1962})))

(def q2 (select :yr (from :movie) (where {:title "Citizen Kane"})))

(def q3
  (select [:id :title :yr]
          (from :movie)
          (where [:like :title "Star Trek%"])
          (order-by :yr)))

(def q4
  (select :title
          (from :movie)
          (where [:in :id [vals 11768 11955 21191]])))

(def q5 (select :id (from :actor) (where {:name "Glenn Close"})))

(def q6 (select :id (from :movie) (where {:title "Casablanca"})))

(def q7
  (select :name
          (from :casting :actor)
          (where {:movieid (select :id (from :movie)
                                   (where {:title "Casablanca"}))
                  :actorid :actor.id})))

(def q7-b
  (select :name
          (from :casting (join :actor :on {:actorid :actor.id}))
          (where {:movieid (select :id (from :movie)
                                   (where {:title "Casablanca"}))})))

(def q8
  (select :name
          (from :movie :casting :actor)
          (where {:title "Alien"
                  :movieid :movie.id
                  :actorid :actor.id})))

(def q8-b
  (select :name
          (from :movie
                (join :casting :on {:movieid :movie.id})
                (join :actor :on {:actorid :actor.id}))
          (where {:title "Alien"})))

(def q9
  (select :title
          (from :movie :casting :actor)
          (where '{name "Harrison Ford"
                   movieid movie.id
                   actorid actor.id})))

(def q9-b
  (select :title
          (from :movie
                (join :casting :on {:movieid :movie.id})
                (join :actor :on {:actorid :actor.id}))
          (where '{name "Harrison Ford"})))

(def q10
  (select :title
          (from :movie :casting :actor)
          (where {:name "Harrison Ford"
                  :movieid :movie.id
                  :actorid :actor.id
                  :ord [not= 1]})))

(def q10-b
  (select :title
          (from :movie
                (join :casting :on {:movieid :movie.id})
                (join :actor :on {:actorid :actor.id}))
          (where {:name "Harrison Ford", :ord [not= 1]})))

(def q11
  (select [:title :name]
          (from :movie :casting :actor)
          (where {:yr 1962
                  :movieid :movie.id
                  :actorid :actor.id
                  :ord 1})))

(def q11-b
  (select [:title :name]
          (from :movie
                (join :casting :on {:movieid :movie.id})
                (join :actor :on {:actorid :actor.id}))
          (where {:yr 1962, :ord 1})))

(def q12
  (select
    [:yr [count :title]]
    (from :movie
          (join :casting :on {:movie.id :movieid})
          (join :actor :on {:actorid :actor.id}))
    (where {:name "John Travolta"})
    (group :yr)
    (having
      [= [count :title]
         (select
           [[max :c]]
           (from (select
                   [:yr [count :title] :as :c]
                   (from :movie
                         (join :casting :on {:movie.id :movieid})
                         (join :actor :on {:actor.id :actorid}))
                   (where {:name "John Travolta"})
                   (group :yr)) :as :t))])))

(def q13
  (select
    [:title :name]
    (from :movie :casting :actor)
    (where
      `(and (= movieid movie.id)
            (= actorid actor.id)
            (= ord 1)
            (in movieid ~(select :movieid
                                 (from :casting :actor)
                                 (where {:actorid :actor.id
                                         :name "Julie Andrews"})))))))

(def q13-b
  (select
    [:title :name]
    (from :movie
          (join :casting :on {:movieid :movie.id})
          (join :actor :on {:actorid :actor.id}))
    (where
      `(and (= ord 1)
            (in movieid ~(select :movieid
                                 (from :casting
                                       (join :actor :on {:actorid :actor.id}))
                                 (where {:name "Julie Andrews"})))))))

(def q13-c
  (select
    [:title :name]
    (from (select-distinct
            [:title :movie.id]
            (from :movie
                  (join :casting :on {:movieid :movie.id})
                  (join :actor :on {:actorid :actor.id}))
            (where {:name "Julie Andrews"})) (as :ja)
          (join :casting :on {:ja.id :movieid})
          (join :actor :on {:actorid :actor.id}))
    (where {:ord 1})))

(def q14
  (select :name
          (from :casting
                (join :actor :on {:actorid :actor.id}))
          (where {:ord 1})
          (group :name)
          (having [>= [count :movieid] 30])))

(def q15
  (select [:title [count :actorid]]
          (from :casting :movie)
          (where {:yr 1978, :movieid :movie.id})
          (group :title)
          (order-by (desc 2))))

(def q15-b
  (select [:title [count :actorid]]
          (from :casting
                (join :movie :on {:movieid :movie.id}))
          (where {:yr 1978})
          (group :title)
          (order-by (desc 2))))

(def q16
  (select-distinct
    :d.name
    (from :actor :as :d
          (join :casting :as :a :on {:a.actorid :d.id})
          (join :casting :as :b :on {:a.movieid :b.movieid})
          (join :actor :as :c :on {:b.actorid :c.id
                                   :c.name "Art Garfunkel" }))
    (where '(not= d.id c.id))))

(def q16-b
  (select
    :name
    (from (select-distinct
            :movie.id
            (from :movie
                  (join :casting :on {:movieid :movie.id})
                  (join :actor :on {:actorid :actor.id}))
            (where {:name "Art Garfunkel"})) (as :ag)
          (join :casting :on {:ag.id :movieid})
          (join :actor :on {:actorid :actor.id}))
    (where [not= :name "Art Garfunkel"])))

