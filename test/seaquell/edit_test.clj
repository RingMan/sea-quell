(ns seaquell.edit-test
  "Demonstrates how to use DSL functions to alter `select` statements"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.edit :refer :all]))

(fact "You can alter a WHERE clause"
  (let [q (select :* (from :t) (where {:x [> 5]}))
        qa (select :* (from :t) (where [:and [> :x 5] [< :y 3]]))
        qo (select :* (from :t) (where [:or [> :x 5] [< :y 3]]))
        qx (select :* (from :t) (where [:xor [> :x 5] [< :y 3]]))]
    (select$ q (and-where {:y [< 3]})) => "SELECT * FROM t WHERE x > 5 AND y < 3;"
    (select$ q (or-where {:y [< 3]})) => "SELECT * FROM t WHERE x > 5 OR y < 3;"
    (select$ q (xor-where {:y [< 3]})) => "SELECT * FROM t WHERE x > 5 XOR y < 3;"
    (select$ q (not-where)) => "SELECT * FROM t WHERE NOT (x > 5);"
    (select$ qa (and-where {:z [= 4]})) => "SELECT * FROM t WHERE x > 5 AND y < 3 AND z = 4;"
    (select$ qo (or-where {:z [= 4]})) => "SELECT * FROM t WHERE x > 5 OR y < 3 OR z = 4;"
    (select$ qx (xor-where {:z [= 4]})) => "SELECT * FROM t WHERE x > 5 XOR y < 3 XOR z = 4;"))

