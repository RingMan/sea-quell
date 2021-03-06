(ns seaquell.window-fn-test
  "Uses an in-memory Sqlite database to do the SQL exercises at
  https://en.wikibooks.org/wiki/SQL_Exercises/Pieces_and_providers"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.engine :refer [db-conn]]
            [seaquell.sqlite :refer [db-spec]]
            [seaquell.to-sql :refer [bound-to-sql expr-to-sql over-clause
                                     win-to-sql windef-to-sql window-clause]]))

(fact "You can specify a starting or ending frame boundary"
  (let [->sql #(-> % :bound bound-to-sql)]
    (->sql current-row) => "CURRENT ROW"
    (->sql (unbounded :following)) => "UNBOUNDED FOLLOWING"
    (->sql unbounded-following) => "UNBOUNDED FOLLOWING"
    (->sql (unbounded :preceding)) => "UNBOUNDED PRECEDING"
    (->sql unbounded-preceding) => "UNBOUNDED PRECEDING"
    (->sql (preceding -expr-)) => "-expr- PRECEDING"
    (provided (expr-to-sql -expr-) => "-expr-")
    (->sql (following -expr-)) => "-expr- FOLLOWING"
    (provided (expr-to-sql -expr-) => "-expr-")))

(fact "You can supply both frame boundaries or just the starting boundary"
  (let [wd (windef (order-by :c) (frame :range))
        wd-s "ORDER BY c RANGE "
        ->sql windef-to-sql]
    (->sql (windef wd (bounds (preceding 1) (following 1)))) =>
    (str wd-s "BETWEEN 1 PRECEDING AND 1 FOLLOWING")
    (->sql (windef wd (lo-bound (preceding 1)) (hi-bound (following 1)))) =>
    (str wd-s "BETWEEN 1 PRECEDING AND 1 FOLLOWING")
    (->sql (windef wd (bounds (unbounded :preceding)))) =>
    (str wd-s "BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW")
    (->sql (windef wd (unbounded :preceding))) => (str wd-s "UNBOUNDED PRECEDING")))

(fact "You can add an EXCLUDE clause to a window frame"
  (let [wd (windef (order-by :c) (frame :groups))
        wd-s "ORDER BY c GROUPS "
        ->sql windef-to-sql]
    (->sql (windef wd (exclude :no-others))) => (str wd-s "EXCLUDE NO OTHERS")
    (->sql (windef wd exclude-no-others)) => (str wd-s "EXCLUDE NO OTHERS")
    (->sql (windef wd (exclude :current-row))) => (str wd-s "EXCLUDE CURRENT ROW")
    (->sql (windef wd exclude-current-row)) => (str wd-s "EXCLUDE CURRENT ROW")
    (->sql (windef wd (exclude :group))) => (str wd-s "EXCLUDE GROUP")
    (->sql (windef wd exclude-group)) => (str wd-s "EXCLUDE GROUP")
    (->sql (windef wd (exclude :ties))) => (str wd-s "EXCLUDE TIES")
    (->sql (windef wd exclude-ties)) => (str wd-s "EXCLUDE TIES")))

(fact "You can specify a window definition for a WINDOW or OVER clause"
  (let [->sql windef-to-sql]
    (->sql (windef (order-by :c))) => "ORDER BY c"
    (->sql (windef (partition-by :c))) => "PARTITION BY c"
    (->sql (windef (partition-by :c1) (order-by :c2) (frame :range)
                   (bounds unbounded-preceding current-row) exclude-no-others)) =>
    (str "PARTITION BY c1 ORDER BY c2 RANGE "
         "BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE NO OTHERS")
    (->sql (windef :win (order-by :c))) => "win ORDER BY c"
    (->sql (windef :win (partition-by :c))) => "win PARTITION BY c"
    (->sql (windef :win (partition-by :c1) (order-by :c2) (frame :range)
                   (bounds unbounded-preceding current-row) exclude-no-others)) =>
    (str "win PARTITION BY c1 ORDER BY c2 RANGE "
         "BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE NO OTHERS")
    (fact "windef is idempotent"
      (windef (windef (order-by :c))) => (windef (order-by :c)))))

(fact "You can use `win` to specify a named window for a WINDOW clause"
  (let [->sql win-to-sql]
    (->sql (win :w :as (windef (order-by :a)))) => "w AS (ORDER BY a)")
  (fact "`win` is idempotent"
    (win (win :w :as (windef (order-by :a)))) =>
    (win :w :as (windef (order-by :a)))))

(fact "You can specify a WINDOW clause"
  (let [->sql #(-> % :window window-clause)]
    (->sql (window (win :w :as (windef (order-by :a))))) =>
    "WINDOW w AS (ORDER BY a)"
    (->sql (window (win :w (as (windef (order-by :a)))))) =>
    "WINDOW w AS (ORDER BY a)"
    (->sql (window :w :as (windef (order-by :a)))) =>
    "WINDOW w AS (ORDER BY a)"
    (->sql (window :w (as (windef (order-by :a))))) =>
    "WINDOW w AS (ORDER BY a)"
    (->sql (window (raw :w :as '(order-by a)))) =>
    "WINDOW w AS (ORDER BY a)"
    (->sql (window "illegal")) => (throws #"Illegal window clause")))

(fact "You can specify an OVER clause"
  (let [->sql over-clause]
    (->sql (over :win)) => "OVER win"
    (->sql (over (windef :win (partition-by :c)))) => "OVER (win PARTITION BY c)"
    (->sql (over (partition-by :c))) => "OVER (PARTITION BY c)"
    (->sql (over (order-by :c))) => "OVER (ORDER BY c)"))

(defn create-tbls [con]
  (create-table! :t0 [[:x INTEGER PRIMARY-KEY] [:y TEXT]] (db con))
  (create-table! :t1 [[:a INTEGER PRIMARY-KEY] :b :c] (db con))
  (create-table! :t2 [:a :b] (db con)))

(defn insert-data [con]
  (let [t0 (insert :t0 (db con))
        t1 (insert :t1 (db con))
        t2 (insert :t2 (db con))]
    (insert! t0 (values [1 "aaa"] [2 "ccc"] [3 "bbb"]))
    (insert! t1 (values [1 \A "one"]
                        [2 \B "two"]
                        [3 \C "three"]
                        [4 \D "one"]
                        [5 \E "two"]
                        [6 \F "three"]
                        [7 \G "one"]))
    (insert! t2 (values [\a "one"]
                        [\a "two"]
                        [\a "three"]
                        [\b "four"]
                        [\c "five"]
                        [\c "six"]))))

(defn mk-db []
  (let [c (db-conn (db-spec))]
    (create-tbls c)
    (insert-data c)
    c))

(let [c (mk-db)]
  (fact "simple window fn works"
        (select! [:x :y (field [:row_number] (over (order-by :y)) (as :row_number))]
                 (from :t0) (order-by :x) (db c))
        =>
        [{:x 1, :y "aaa", :row_number 1}
         {:x 2, :y "ccc", :row_number 3}
         {:x 3, :y "bbb", :row_number 2}
         ]
        ))

(let [c (mk-db)]
  (fact
    "aggregate window fns work"

    ;; 2. Aggregate Window Functions
    (fact
      (select!
        [:a :b (field [:group_concat :b "."]
                      (over (order-by :a) (frame :rows) (bounds (preceding 1) (following 1)))
                      (as :group_concat))]
        (from :t1) (db c))
      =>
      [{:a 1, :b "A", :group_concat "A.B"}
       {:a 2, :b "B", :group_concat "A.B.C"}
       {:a 3, :b "C", :group_concat "B.C.D"}
       {:a 4, :b "D", :group_concat "C.D.E"}
       {:a 5, :b "E", :group_concat "D.E.F"}
       {:a 6, :b "F", :group_concat "E.F.G"}
       {:a 7, :b "G", :group_concat "F.G"}])

    ;; 2.1 The PARTITION BY Clause
    ;; Example 1
    (fact
      (select!
        [:c :a :b
         (field [:group_concat :b "."]
                (over (partition-by :c) (order-by :a)
                      (frame :range) (bounds current-row unbounded-following))
                (as :group_concat))]
        (from :t1) (order-by :c :a) (db c))
      =>
      [{:c "one"   :a 1, :b "A", :group_concat "A.D.G"}
       {:c "one"   :a 4, :b "D", :group_concat "D.G"}
       {:c "one"   :a 7, :b "G", :group_concat "G"}
       {:c "three" :a 3, :b "C", :group_concat "C.F"}
       {:c "three" :a 6, :b "F", :group_concat "F"}
       {:c "two"   :a 2, :b "B", :group_concat "B.E"}
       {:c "two"   :a 5, :b "E", :group_concat "E"}])

    ;; Example 2
    (fact
      (select!
        [:c :a :b
         (field [:group_concat :b "."]
                (over (partition-by :c) (order-by :a)
                      (frame :range) (bounds current-row unbounded-following))
                (as :group_concat))]
        (from :t1) (order-by :a) (db c))
      =>
      [{:c "one"   :a 1, :b "A", :group_concat "A.D.G"}
       {:c "two"   :a 2, :b "B", :group_concat "B.E"}
       {:c "three" :a 3, :b "C", :group_concat "C.F"}
       {:c "one"   :a 4, :b "D", :group_concat "D.G"}
       {:c "two"   :a 5, :b "E", :group_concat "E"}
       {:c "three" :a 6, :b "F", :group_concat "F"}
       {:c "one"   :a 7, :b "G", :group_concat "G"}])

    ;; 2.2 Frame Specifications
    ;; Default frame spec
    (fact
      (select!
        [:a :b :c
         (field [:group_concat :b "."] (over (order-by :c)) (as :group_concat))]
        (from :t1) (order-by :a) (db c))
      =>
      [{:c "one"   :a 1, :b "A", :group_concat "A.D.G"}
       {:c "two"   :a 2, :b "B", :group_concat "A.D.G.C.F.B.E"}
       {:c "three" :a 3, :b "C", :group_concat "A.D.G.C.F"}
       {:c "one"   :a 4, :b "D", :group_concat "A.D.G"}
       {:c "two"   :a 5, :b "E", :group_concat "A.D.G.C.F.B.E"}
       {:c "three" :a 6, :b "F", :group_concat "A.D.G.C.F"}
       {:c "one"   :a 7, :b "G", :group_concat "A.D.G"}])

    ;; 2.2.2 Frame Boundaries
    (fact
      (select!
        [:c :a :b
         (field [:group_concat :b "."]
                (over (order-by :c :a)
                      (frame :rows) (bounds current-row unbounded-following))
                (as :group_concat))]
        (from :t1) (order-by :c :a) (db c))
      =>
      [{:c "one"   :a 1, :b "A", :group_concat "A.D.G.C.F.B.E"}
       {:c "one"   :a 4, :b "D", :group_concat "D.G.C.F.B.E"}
       {:c "one"   :a 7, :b "G", :group_concat "G.C.F.B.E"}
       {:c "three" :a 3, :b "C", :group_concat "C.F.B.E"}
       {:c "three" :a 6, :b "F", :group_concat "F.B.E"}
       {:c "two"   :a 2, :b "B", :group_concat "B.E"}
       {:c "two"   :a 5, :b "E", :group_concat "E"}])

    ;; 2.2.3 The EXCLUDE Clause
    (fact
      (select!
        (let [f (field
                  [:group_concat :b "."]
                  (over (order-by :c)
                        (frame :groups) (bounds unbounded-preceding current-row)))]
          [:c :a :b
           (field f (exclude :no-others) (as :no_others))
           (field f (exclude :current-row) (as :current_row))
           (field f (exclude :group) (as :grp))
           (field f (exclude :ties) (as :ties))])
        (from :t1) (order-by :c :a) (db c))
      =>
      [{:c "one"   :a 1, :b "A", :no_others "A.D.G",         :current_row "D.G",         :grp nil,          :ties "A"}
       {:c "one"   :a 4, :b "D", :no_others "A.D.G",         :current_row "A.G",         :grp nil,          :ties "D"}
       {:c "one"   :a 7, :b "G", :no_others "A.D.G",         :current_row "A.D",         :grp nil,          :ties "G"}
       {:c "three" :a 3, :b "C", :no_others "A.D.G.C.F",     :current_row "A.D.G.F",     :grp "A.D.G",     :ties "A.D.G.C"}
       {:c "three" :a 6, :b "F", :no_others "A.D.G.C.F",     :current_row "A.D.G.C",     :grp "A.D.G",     :ties "A.D.G.F"}
       {:c "two"   :a 2, :b "B", :no_others "A.D.G.C.F.B.E", :current_row "A.D.G.C.F.E", :grp "A.D.G.C.F", :ties "A.D.G.C.F.B"}
       {:c "two"   :a 5, :b "E", :no_others "A.D.G.C.F.B.E", :current_row "A.D.G.C.F.B", :grp "A.D.G.C.F", :ties "A.D.G.C.F.E"}])

    ;; 2.3 The FILTER Clause
    (fact
      (select!
        [:c :a :b
         (field [:group_concat :b "."] (as :group_concat)
                (filter-where {:c [not= "two"]}) (over (order-by :a)))]
        (from :t1) (order-by :a) (db c))
      =>
      [{:c "one"   :a 1, :b "A", :group_concat "A"}
       {:c "two"   :a 2, :b "B", :group_concat "A"}
       {:c "three" :a 3, :b "C", :group_concat "A.C"}
       {:c "one"   :a 4, :b "D", :group_concat "A.C.D"}
       {:c "two"   :a 5, :b "E", :group_concat "A.C.D"}
       {:c "three" :a 6, :b "F", :group_concat "A.C.D.F"}
       {:c "one"   :a 7, :b "G", :group_concat "A.C.D.F.G"}])

    (fact
      (select! [:a :as :a
                (field [:row_number] (over :win) (as :row_number))
                (field [:rank] (over :win) (as :rank))
                (field [:dense_rank] (over :win) (as :dense_rank))
                (field [:percent_rank] (over :win) (as :percent_rank))
                (field [:cume_dist] (over :win) (as :cume_dist))]
               (db c)
               (from :t2)
               (window (win :win :as (windef (order-by :a)))))
      =>
      [{:a "a", :row_number 1, :rank 1, :dense_rank 1, :percent_rank 0.0, :cume_dist 0.5}
       {:a "a", :row_number 2, :rank 1, :dense_rank 1, :percent_rank 0.0, :cume_dist 0.5}
       {:a "a", :row_number 3, :rank 1, :dense_rank 1, :percent_rank 0.0, :cume_dist 0.5}
       {:a "b", :row_number 4, :rank 4, :dense_rank 2, :percent_rank 0.6, :cume_dist 0.6666666666666666}
       {:a "c", :row_number 5, :rank 5, :dense_rank 3, :percent_rank 0.8, :cume_dist 1.0}
       {:a "c", :row_number 6, :rank 5, :dense_rank 3, :percent_rank 0.8, :cume_dist 1.0}])

    (fact
      (select! [:a :as :a
                :b :as :b
                (field [:ntile 2] (over :win) (as :ntile_2))
                (field [:ntile 4] (over :win) (as :ntile_4))]
               (db c)
               (from :t2)
               (window (win :win :as (windef (order-by :a)))))
      =>
      [{:a "a", :b "one",   :ntile_2 1, :ntile_4 1}
       {:a "a", :b "two",   :ntile_2 1, :ntile_4 1}
       {:a "a", :b "three", :ntile_2 1, :ntile_4 2}
       {:a "b", :b "four",  :ntile_2 2, :ntile_4 2}
       {:a "c", :b "five",  :ntile_2 2, :ntile_4 3}
       {:a "c", :b "six",   :ntile_2 2, :ntile_4 4}])

    (fact
      (select! [:b :as :b
                (field [:lead :b 2 "n/a"] (over :win) (as :lead))
                (field [:lag :b] (over :win) (as :lag))
                (field [:first_value :b] (over :win) (as :first_value))
                (field [:last_value :b] (over :win) (as :last_value))
                (field [:nth_value :b 3] (over :win) (as :nth_value_3))]
               (db c)
               (from :t1)
               (window
                 (win :win :as
                      (windef
                        (order-by :b)
                        (frame :rows) (bounds unbounded-preceding current-row)))))
      =>
      [{:b "A", :lead "C",   :lag nil,  :first_value "A", :last_value "A", :nth_value_3 nil}
       {:b "B", :lead "D",   :lag "A" , :first_value "A", :last_value "B", :nth_value_3 nil}
       {:b "C", :lead "E",   :lag "B" , :first_value "A", :last_value "C", :nth_value_3 "C"}
       {:b "D", :lead "F",   :lag "C" , :first_value "A", :last_value "D", :nth_value_3 "C"}
       {:b "E", :lead "G",   :lag "D" , :first_value "A", :last_value "E", :nth_value_3 "C"}
       {:b "F", :lead "n/a", :lag "E" , :first_value "A", :last_value "F", :nth_value_3 "C"}
       {:b "G", :lead "n/a", :lag "F" , :first_value "A", :last_value "G", :nth_value_3 "C"}
       ])

    ))
