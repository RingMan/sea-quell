(ns seaquell.raw-fn-test
  "Demonstrates many of the places where you can use the `raw` function
  to supply a literal SQL fragment."
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.to-sql :refer [raw-to-sql]]))

(fact "You can use the `raw` fn to create SQL fragments. Leading strings are
      treated as verbatim SQL"
  (let [->sql raw-to-sql]
    (->sql (raw "literal SQL;")) => "literal SQL;"
    (->sql (raw "insert" "into t" "values(1, 2, 3);")) =>
    "insert into t values(1, 2, 3);"
    (->sql (raw '[x + y * (w - z)])) => "x + y * (w - z)"
    ))

(fact "You can use `raw` anywhere you would use a name"
  (sql$ (analyze (raw "\"my-tbl\""))) => "ANALYZE \"my-tbl\";")
