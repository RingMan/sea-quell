(ns seaquell.window-fn-test
  "Uses an in-memory Sqlite database to do the SQL exercises at
  https://en.wikibooks.org/wiki/SQL_Exercises/Pieces_and_providers"
  (:refer-clojure :exclude [update partition-by])
  (:require [clojure.java.jdbc :as jdb])
  (:require [diesel.core :refer [mk-map*]])
  (:use midje.sweet
        seaquell.core
        seaquell.engine))

(def sq3 {:classname "org.sqlite.JDBC"
          :subprotocol "sqlite"
          :subname ":memory:"})

(defn create-tbls [con]
  (do-sql
    "CREATE TABLE t0 (x INTEGER PRIMARY KEY, y TEXT);"
    (db con))
  (do-sql
    "CREATE TABLE t1 (a INTEGER PRIMARY KEY, b, c);"
    (db con)))

(defn insert-data [con]
  (let [t0 (insert :t0 (db con))
        t1 (insert :t1 (db con))]
    (insert! t0 (values [1 "aaa"] [2 "ccc"] [3 "bbb"]))
    (insert! t1 (values [1 \A "one"]
                        [2 \B "two"]
                        [3 \C "three"]
                        [4 \D "one"]
                        [5 \E "two"]
                        [6 \F "three"]
                        [7 \G "one"]))))

(defn mk-db []
  (let [c (->> sq3 jdb/get-connection
               (jdb/add-connection sq3))]
    (create-tbls c)
    (insert-data c)
    c))

(let [c (mk-db)]
  (fact "simple window fn works"
        (select! [:x :y (raw "row_number() OVER (ORDER BY y)") (as :row_number)]
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
    (select!
      [:a :b
       (raw "group_concat(b, '.') OVER (ORDER BY a ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING)")
       (as :group_concat)]
      (from :t1) (db c))
    =>
    [{:a 1, :b "A", :group_concat "A.B"}
     {:a 2, :b "B", :group_concat "A.B.C"}
     {:a 3, :b "C", :group_concat "B.C.D"}
     {:a 4, :b "D", :group_concat "C.D.E"}
     {:a 5, :b "E", :group_concat "D.E.F"}
     {:a 6, :b "F", :group_concat "E.F.G"}
     {:a 7, :b "G", :group_concat "F.G"}
     ]

    ;; 2.1 The PARTITION BY Clause
    ;; Example 1
    (select!
      [:c :a :b
       (raw "group_concat(b, '.') OVER
            (PARTITION BY c ORDER BY a RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING)")
       (as :group_concat)]
      (from :t1) (order-by :c :a) (db c))
    =>
    [{:c "one"   :a 1, :b "A", :group_concat "A.D.G"}
     {:c "one"   :a 4, :b "D", :group_concat "D.G"}
     {:c "one"   :a 7, :b "G", :group_concat "G"}
     {:c "three" :a 3, :b "C", :group_concat "C.F"}
     {:c "three" :a 6, :b "F", :group_concat "F"}
     {:c "two"   :a 2, :b "B", :group_concat "B.E"}
     {:c "two"   :a 5, :b "E", :group_concat "E"}]

    ;; Example 2
    (select!
      [:c :a :b
       (raw "group_concat(b, '.') OVER
            (PARTITION BY c ORDER BY a RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING)")
       (as :group_concat)]
      (from :t1) (order-by :a) (db c))
    =>
    [{:c "one"   :a 1, :b "A", :group_concat "A.D.G"}
     {:c "two"   :a 2, :b "B", :group_concat "B.E"}
     {:c "three" :a 3, :b "C", :group_concat "C.F"}
     {:c "one"   :a 4, :b "D", :group_concat "D.G"}
     {:c "two"   :a 5, :b "E", :group_concat "E"}
     {:c "three" :a 6, :b "F", :group_concat "F"}
     {:c "one"   :a 7, :b "G", :group_concat "G"}]

    ;; 2.2 Frame Specifications
    ;; Default frame spec
    (select!
      [:a :b :c (raw "group_concat(b, '.') OVER (ORDER BY c)") (as :group_concat)]
      (from :t1) (order-by :a) (db c))
    =>
    [{:c "one"   :a 1, :b "A", :group_concat "A.D.G"}
     {:c "two"   :a 2, :b "B", :group_concat "A.D.G.C.F.B.E"}
     {:c "three" :a 3, :b "C", :group_concat "A.D.G.C.F"}
     {:c "one"   :a 4, :b "D", :group_concat "A.D.G"}
     {:c "two"   :a 5, :b "E", :group_concat "A.D.G.C.F.B.E"}
     {:c "three" :a 6, :b "F", :group_concat "A.D.G.C.F"}
     {:c "one"   :a 7, :b "G", :group_concat "A.D.G"}]

    ;; 2.2.2 Frame Boundaries
    (select!
      [:c :a :b
       (raw "group_concat(b, '.') OVER
            (ORDER BY c, a ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING)")
       (as :group_concat)]
      (from :t1) (order-by :c :a) (db c))
    =>
    [{:c "one"   :a 1, :b "A", :group_concat "A.D.G.C.F.B.E"}
     {:c "one"   :a 4, :b "D", :group_concat "D.G.C.F.B.E"}
     {:c "one"   :a 7, :b "G", :group_concat "G.C.F.B.E"}
     {:c "three" :a 3, :b "C", :group_concat "C.F.B.E"}
     {:c "three" :a 6, :b "F", :group_concat "F.B.E"}
     {:c "two"   :a 2, :b "B", :group_concat "B.E"}
     {:c "two"   :a 5, :b "E", :group_concat "E"}]

    ;; 2.2.3 The EXCLUDE Clause
    (select!
      [:c :a :b
       (raw "group_concat(b, '.') OVER
            (ORDER BY c GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE NO OTHERS)")
       (as :no_others)
       (raw "group_concat(b, '.') OVER
            (ORDER BY c GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE CURRENT ROW)")
       (as :current_row)
       (raw "group_concat(b, '.') OVER
            (ORDER BY c GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE GROUP)")
       (as :grp)
       (raw "group_concat(b, '.') OVER
            (ORDER BY c GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE TIES)")
       (as :ties)
       ]
      (from :t1) (order-by :c :a) (db c))
    =>
    [{:c "one"   :a 1, :b "A", :no_others "A.D.G",         :current_row "D.G",         :grp nil,          :ties "A"}
     {:c "one"   :a 4, :b "D", :no_others "A.D.G",         :current_row "A.G",         :grp nil,          :ties "D"}
     {:c "one"   :a 7, :b "G", :no_others "A.D.G",         :current_row "A.D",         :grp nil,          :ties "G"}
     {:c "three" :a 3, :b "C", :no_others "A.D.G.C.F",     :current_row "A.D.G.F",     :grp "A.D.G",     :ties "A.D.G.C"}
     {:c "three" :a 6, :b "F", :no_others "A.D.G.C.F",     :current_row "A.D.G.C",     :grp "A.D.G",     :ties "A.D.G.F"}
     {:c "two"   :a 2, :b "B", :no_others "A.D.G.C.F.B.E", :current_row "A.D.G.C.F.E", :grp "A.D.G.C.F", :ties "A.D.G.C.F.B"}
     {:c "two"   :a 5, :b "E", :no_others "A.D.G.C.F.B.E", :current_row "A.D.G.C.F.B", :grp "A.D.G.C.F", :ties "A.D.G.C.F.E"}]

    ;; 2.3 The FILTER Clause
    (select!
      [:c :a :b
       (raw "group_concat(b, '.') FILTER (WHERE c!='two') OVER (ORDER BY a)")
       (as :group_concat)]
      (from :t1) (order-by :a) (db c))
    =>
    [{:c "one"   :a 1, :b "A", :group_concat "A"}
     {:c "two"   :a 2, :b "B", :group_concat "A"}
     {:c "three" :a 3, :b "C", :group_concat "A.C"}
     {:c "one"   :a 4, :b "D", :group_concat "A.C.D"}
     {:c "two"   :a 5, :b "E", :group_concat "A.C.D"}
     {:c "three" :a 6, :b "F", :group_concat "A.C.D.F"}
     {:c "one"   :a 7, :b "G", :group_concat "A.C.D.F.G"}]

    ))
