(ns seaquell.analyze-test
  "Tests SQLite ANALYZE statement"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]))

(fact "Seaquell supports the ANALYZE statement"
  (fact "You can analyze an entire database, an attached schema or a
        single table or index"
    (analyze$) => "ANALYZE;"
    (analyze$ :main) => "ANALYZE main;"
    (analyze$ (schema :main)) => "ANALYZE main;"
    (analyze$ :tbl_or_idx) => "ANALYZE tbl_or_idx;"
    (analyze$ :main.tbl_or_idx) => "ANALYZE main.tbl_or_idx;")
  (fact "analyze is idempotent"
    (analyze (analyze :main)) => (analyze :main)))

