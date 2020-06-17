(ns seaquell.window-fn-test
  "Uses an in-memory Sqlite database to do the SQL exercises at
  https://en.wikibooks.org/wiki/SQL_Exercises/Pieces_and_providers"
  (:refer-clojure :exclude [distinct drop group-by into update partition-by])
  (:require [clojure.java.jdbc :as jdb]
            [diesel.core :refer [mk-map*]]
            [diesel.edit :refer [edit-in]]
            [midje.sweet :refer :all]
            [seaquell.core :refer :all]
            [seaquell.engine :refer :all]
            [seaquell.sqlite :refer [db-spec]]))

(defn create-tbls [con]
  (do-sql
    "CREATE TABLE t0 (x INTEGER PRIMARY KEY, y TEXT);"
    (db con))
  (do-sql
    "CREATE TABLE t1 (a INTEGER PRIMARY KEY, b, c);"
    (db con))
  (do-sql
    "CREATE TABLE t2 (a, b);"
    (db con)))

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
