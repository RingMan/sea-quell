(ns seaquell.select-test
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.to-sql :refer [join-op-to-sql order-by-clause order-item]]))

(fact "You can specify a JOIN clause"
  (let [->sql join-op-to-sql]
    (->sql (cross-join :t (using :c))) => "CROSS JOIN t USING (c)"
    (->sql (inner-join :t (using :c))) => "INNER JOIN t USING (c)"
    (->sql (left-join :t (using :c))) => "LEFT JOIN t USING (c)"
    (->sql (right-join :t (using :c))) => "RIGHT JOIN t USING (c)"
    (->sql (full-join :t (using :c))) => "FULL JOIN t USING (c)"
    (->sql (left-outer-join :t (using :c))) => "LEFT OUTER JOIN t USING (c)"
    (->sql (right-outer-join :t (using :c))) => "RIGHT OUTER JOIN t USING (c)"
    (->sql (full-outer-join :t (using :c))) => "FULL OUTER JOIN t USING (c)"
    (->sql (natural-join :t (using :c))) => "NATURAL JOIN t USING (c)"
    (->sql (natural-cross-join :t (using :c))) => "NATURAL CROSS JOIN t USING (c)"
    (->sql (natural-inner-join :t (using :c))) => "NATURAL INNER JOIN t USING (c)"
    (->sql (natural-left-join :t (using :c))) => "NATURAL LEFT JOIN t USING (c)"
    (->sql (natural-right-join :t (using :c))) => "NATURAL RIGHT JOIN t USING (c)"
    (->sql (natural-full-join :t (using :c))) => "NATURAL FULL JOIN t USING (c)"
    (->sql (natural-left-outer-join :t (using :c))) => "NATURAL LEFT OUTER JOIN t USING (c)"
    (->sql (natural-right-outer-join :t (using :c))) => "NATURAL RIGHT OUTER JOIN t USING (c)"
    (->sql (natural-full-outer-join :t (using :c))) => "NATURAL FULL OUTER JOIN t USING (c)"
    (->sql (straight-join :t (using :c))) => "STRAIGHT_JOIN t USING (c)"
    (->sql (comma-join :t)) => ", t"
    (->sql (nil-join :t)) => "t"))

(fact "You can specify ordering terms for the ORDER BY clause"
  (let [->sql order-item]
    (fact "A column name, column number, or expression by itself can be an ordering term"
      (->sql :c1) => "c1"
      (->sql 2) => "2"
      (->sql [* :x :y]) => "x * y")
    (fact "You can order ascending or descending"
      (->sql (order-term :c1 (asc))) => "c1 ASC"
      (->sql (order-term :c1 ASC)) => "c1 ASC"
      (->sql (order-term :c1 (order :desc))) => "c1 DESC"
      (->sql (order-term :c1 (desc))) => "c1 DESC"
      (->sql (order-term :c1 DESC)) => "c1 DESC"
      (->sql (order-term :c1 (order :asc))) => "c1 ASC"
      (->sql (asc (expr :c1))) => "c1 ASC"
      (->sql (asc :c1)) => "c1 ASC"
      (->sql (asc 2)) => "2 ASC"
      (->sql (asc [* :x :y])) => "x * y ASC"
      (->sql (desc (expr :c1))) => "c1 DESC"
      (->sql (desc :c1)) => "c1 DESC"
      (->sql (desc 2)) => "2 DESC"
      (->sql (desc [* :x :y])) => "x * y DESC")
    (fact "You can sort nulls first or last"
      (->sql (order-term :c1 (nulls :first))) => "c1 NULLS FIRST"
      (->sql (order-term :c1 (nulls-first))) => "c1 NULLS FIRST"
      (->sql (order-term :c1 NULLS-FIRST)) => "c1 NULLS FIRST"
      (->sql (order-term :c1 (nulls :last))) => "c1 NULLS LAST"
      (->sql (order-term :c1 (nulls-last))) => "c1 NULLS LAST"
      (->sql (order-term :c1 NULLS-LAST)) => "c1 NULLS LAST"
      (->sql (asc :c1 (nulls :last))) => "c1 ASC NULLS LAST"
      (->sql (desc :c1 (nulls :first))) => "c1 DESC NULLS FIRST")
    (fact "You can specify a collation name to use when sorting"
      (->sql (order-term :c1 (collate :nocase))) => "c1 COLLATE nocase"
      (->sql (asc :c1 (collate :binary))) => "c1 COLLATE binary ASC"
      (->sql (desc :c1 (collate :rtrim))) => "c1 COLLATE rtrim DESC")
    (fact "You can specify collation name, sort order, and null ordering"
      (->sql (order-term :c1 (collate :nocase) (asc) (nulls :last))) =>
      "c1 COLLATE nocase ASC NULLS LAST"
      (->sql (asc :c1 (collate :binary) (nulls :last))) =>
      "c1 COLLATE binary ASC NULLS LAST"
      (->sql (desc :c1 (collate :rtrim) NULLS-FIRST)) =>
      "c1 COLLATE rtrim DESC NULLS FIRST")))

(fact "You can specify an ORDER BY clause with one or more ordering terms"
  (let [->sql #(-> % :order-by order-by-clause)]
    (fact "You can order by column name, column number, or expression"
      (->sql (order-by :c1 :c2)) => "ORDER BY c1, c2"
      (->sql (order-by 2 4)) => "ORDER BY 2, 4"
      (->sql (order-by [* :x :y])) => "ORDER BY x * y")
    (fact "You can order ascending or descending"
      (->sql (order-by (asc :c1) (desc :c2))) => "ORDER BY c1 ASC, c2 DESC"
      (->sql (order-by (asc 2) (desc 4))) => "ORDER BY 2 ASC, 4 DESC"
      (->sql (order-by (asc [* :x :y]))) => "ORDER BY x * y ASC")))

(fact "You can select a single column with or without an alias"
  (select$ 42) => "SELECT 42;"
  (select$ [42 (as :n)]) => "SELECT 42 AS n;"
  (select$ [42 :as :n]) => "SELECT 42 AS n;"
  (select$ (field 42 (as :n))) => "SELECT 42 AS n;")

(fact "You can select a single expression with or without an alias"
  (select$ [[* 6 7]]) => "SELECT 6 * 7;"
  (select$ [[* 6 7] (as :n)]) => "SELECT 6 * 7 AS n;"
  (select$ [[* 6 7] :as :n]) => "SELECT 6 * 7 AS n;"
  (select$ ['(* 6 7)]) => "SELECT 6 * 7;"
  (select$ ['(* 6 7) (as :n)]) => "SELECT 6 * 7 AS n;"
  (select$ ['(* 6 7) :as :n]) => "SELECT 6 * 7 AS n;"
  (select$ (field [* 6 7] (as :n))) => "SELECT 6 * 7 AS n;")

(fact "You can select multiple columns"
  (select$ [:col1 :col2]) => "SELECT col1, col2;"
  (select$ [:col1 (as :c1) :col2 (as :c2)]) => "SELECT col1 AS c1, col2 AS c2;"
  (select$ (fields :col1 :col2)) => "SELECT col1, col2;")

(fact
  "The following select all columns from a table"
  (let [sql "SELECT * FROM t;"]
    (select$ :* (from :t)) => sql
    (select$ :* :from :t)  => sql
    (select-from$ :t)      => sql))

(fact
  "There are two ways to specify a column alias"
  (let [sql "SELECT price AS p FROM t;"]
    (select$ [:price (as :p)] (from :t)) => sql
    (select$ [:price :as :p] (from :t))  => sql))

(fact
  "You can also alias tables or subqueries"
  (let [sql "SELECT * FROM tbl AS t;"]
    (select$ :* (from :tbl (as :t))) => sql
    (select$ :* (from :tbl :as :t)) => sql))

(fact
  "You can use the ALL modifier"
  (let [sql "SELECT ALL fld FROM t;"]
    (select-all$ :fld (from :t)) => sql
    (select$ :fld (all) (from :t)) => sql
    (select$ :fld (modifier :all) (from :t)) => sql))

(fact
  "You can use the DISTINCT modifier"
  (let [sql "SELECT DISTINCT fld FROM t;"]
    (select-distinct$ :fld (from :t)) => sql
    (select$ :fld (distinct) (from :t)) => sql
    (select$ :fld (modifier :distinct) (from :t)) => sql))
