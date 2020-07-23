(ns seaquell.with-test
  "Uses an in-memory Sqlite database to demonstrate a couple of
  'Outlandish Recursive Query Examples' from the Sqlite website:
  https://sqlite.org/lang_with.html"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [clojure.java.jdbc :as jdb]
            [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.engine :refer [row-fn result-set-fn]]
            [seaquell.sqlite :refer [db-spec]]
            [seaquell.to-sql :refer [with-clause]]))

(fact "You can specify a WITH clause"
  (let [->sql #(-> % :with with-clause)
        vs (values [1, 2] [3, 4])
        vs-s "(VALUES (1, 2), (3, 4))"]
    (->sql (with :tbl (as vs))) => (str "WITH tbl AS " vs-s)
    (->sql (with :tbl :as vs)) => (str "WITH tbl AS " vs-s)
    (->sql (with :tbl [:x :y] (as vs))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with :tbl [:x :y] :as vs)) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with :tbl (columns :x :y) (as vs))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with :tbl (columns :x :y) :as vs)) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with :tbl :columns [:x :y] (as vs))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with :tbl :columns [:x :y] :as vs)) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with :tbl :columns [:x :y] :as vs)) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with (cte :tbl (as vs)))) => (str "WITH tbl AS " vs-s)
    (->sql (with (cte :tbl :as vs))) => (str "WITH tbl AS " vs-s)
    (->sql (with (cte :tbl [:x :y] (as vs)))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with (cte :tbl [:x :y] :as vs))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with (cte :tbl (columns :x :y) (as vs)))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with (cte :tbl (columns :x :y) :as vs))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with (cte :tbl :columns [:x :y] (as vs)))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with (cte :tbl :columns [:x :y] :as vs))) => (str "WITH tbl(x, y) AS " vs-s)
    (->sql (with (cte :tbl :columns [:x :y] :as vs))) => (str "WITH tbl(x, y) AS " vs-s)
    (with :tbl [:x :y]) => (throws #"Illegal with clause")
    (fact "cte is idempotent"
      (cte (cte :tbl [:x :y] (as vs))) => (cte :tbl [:x :y] (as vs)))))

(fact "Use `with$` to render a statement with a leading WITH clause"
  (with$ :t [:x] :as (values [1] [2] [3]) (select :x (from :t))) =>
  "WITH t(x) AS (VALUES (1), (2), (3)) SELECT x FROM t;")

(fact "Use `with!` to execute a statement with a leading WITH clause.
      Put the `db` property in the statement itself, not the `with` clause."
  (with! :t [:x] :as (values [1] [2] [3]) (select :x (from :t) (db (db-spec)))) =>
  [{:x 1} {:x 2} {:x 3}])

(fact "You can also treat `with` as just another clause for statements that support it."
  (select$ :x (from :t) (with :t [:x] :as (values [1] [2] [3]))) =>
  "WITH t(x) AS (VALUES (1), (2), (3)) SELECT x FROM t;"
  (select! :x (from :t) (with :t [:x] :as (values [1] [2] [3])) (db (db-spec)))
  => [{:x 1} {:x 2} {:x 3}])

(def sudoku-solver
  (with-recursive
      :input [:sud] :as (value (str "53..7....6..195....98....6.8...6...34..8.3.."
                                    "17...2...6.6....28....419..5....8..79")),
      :digits [:z :lp] :as
      (union-all
        (value "1" 1)
        (select [[:cast [+ :lp 1] :TEXT] [+ :lp 1]]
                (from :digits) (where [< :lp 9]))),
      :x [:s :ind] :as
      (union-all
        (select [:sud [:instr :sud "."]] (from :input))
        (select
          ['(|| (substr s 1 (- ind 1)) z (substr s (+ ind 1)))
           '(instr (|| (substr s 1 (- ind 1)) z (substr s (+ ind 1))) ".")]
          (from :x (comma-join :digits :as :z))
          (where
            [:and
             [> :ind 0]
             [:not-exists
              (select
                1 (from :digits :as :lp)
                (where
                  '(or (= z.z (substr s (+ (* (/ (- ind 1) 9) 9) lp) 1))
                       (= z.z (substr s
                                      (+ (% (- ind 1) 9)
                                         (* (- lp 1) 9)
                                         1)
                                      1))
                       (= z.z (substr s
                                      (+ (* (% (/ (- ind 1) 3) 3) 3)
                                         (* (/ (- ind 1) 27) 27)
                                         lp
                                         (* (/ (- lp 1) 3) 6))
                                      1)))))]])))
      (select :s (from :x) (where {:ind 0}))))

(fact
  (sql! sudoku-solver (db (db-spec)) (row-fn :s) (result-set-fn first))
  => (str "53467891267219534819834256785976142342685379"
          "1713924856961537284287419635345286179"))

(def mandelbrot
  (with-recursive
    :xaxis[:x] :as
    (union-all (value -2.0)
               (select [[+ :x 0.05]] (from :xaxis) (where [< :x 1.2]))),
    :yaxis[:y] :as
    (union-all (value -1.0)
               (select [[+ :y 0.1]] (from :yaxis) (where [< :y 1.0]))),
    :m[:iter :cx :cy :x :y] :as
    (union-all
      (select [0 :x :y 0.0 0.0] (from :xaxis, :yaxis))
      (select
        ['(+ iter 1) :cx :cy '(+ (- (* x x) (* y y)) cx) '(+ (* 2.0 x y) cy)]
        (from :m)
        (where '(and (< (+ (* x x) (* y y)) 4.0) (< iter 28))))),
    :m2[:iter :cx :cy] :as
    (select [[max :iter] :cx :cy] (from :m) (group-by :cx :cy)),
    :a[:t] :as
    (select [[:group_concat '(substr " .+*#" (+ 1 (min (/ iter 7) 4)) 1) ""]] 
            (from :m2) (group-by :cy))
    (select [[:group_concat '(rtrim t) (binary "0a")]] (from :a))))

(fact
  (sql! mandelbrot (db (db-spec)) (result-set-fn (comp val first first)))
  =>
"                                    ....#
                                   ..#*..
                                 ..+####+.
                            .......+####....   +
                           ..##+*##########+.++++
                          .+.##################+.
              .............+###################+.+
              ..++..#.....*#####################+.
             ...+#######++#######################.
          ....+*################################.
 #############################################...
          ....+*################################.
             ...+#######++#######################.
              ..++..#.....*#####################+.
              .............+###################+.+
                          .+.##################+.
                           ..##+*##########+.++++
                            .......+####....   +
                                 ..+####+.
                                   ..#*..
                                    ....#
                                    +.")

