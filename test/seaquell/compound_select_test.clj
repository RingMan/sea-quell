(ns seaquell.compound-select-test
  "Tests compound SELECT statements"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]))

(fact "You can create compound SELECT statements"
  (let [q1 (select :* (from :t1))
        q2 (select :* (from :t2))
        q3 (select :* (from :t3))
        q4 (select :* (from :t4))
        q5 (select :* (from :t5))
        q1-s "SELECT * FROM t1"
        q2-s "SELECT * FROM t2"
        q3-s "SELECT * FROM t3"
        q4-s "SELECT * FROM t4"
        q5-s "SELECT * FROM t5"
        qu (union q1 q2 q3)]
    (union$ q1 q2 q3)
    => (str q1-s " UNION " q2-s " UNION " q3-s \;)
    (union-all$ q1 q2 q3)
    => (str q1-s " UNION ALL " q2-s " UNION ALL " q3-s \;)
    (intersect$ q1 q2 q3)
    => (str q1-s " INTERSECT " q2-s " INTERSECT " q3-s \;)
    (intersect-all$ q1 q2 q3)
    => (str q1-s " INTERSECT ALL " q2-s " INTERSECT ALL " q3-s \;)
    (except$ q1 q2 q3)
    => (str q1-s " EXCEPT " q2-s " EXCEPT " q3-s \;)
    (except-all$ q1 q2 q3)
    => (str q1-s " EXCEPT ALL " q2-s " EXCEPT ALL " q3-s \;)
    (union$ qu (selects q3 q4 q5))
    => (str q3-s " UNION " q4-s " UNION " q5-s \;)
    (fact "compound-select is idempotent"
      (compound-select (compound-select q1 (union q2)))
      => (compound-select q1 (union q2)))))

