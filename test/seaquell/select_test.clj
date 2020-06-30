(ns seaquell.select-test
  (:refer-clojure
    :exclude [distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]))

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
