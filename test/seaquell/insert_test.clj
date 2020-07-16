(ns seaquell.insert-test
  (:refer-clojure :exclude [drop into update partition-by])
  (:require [midje.sweet :refer :all]
            [seaquell.core :refer :all]))

(facts
  "about insert syntax"
  ;; multiple values with or without specifying columns
  (insert$ :tbl (columns :a :b :c) (values [1 2 3] [4 5 6]))
  => "INSERT INTO tbl (a, b, c) VALUES (1, 2, 3), (4, 5, 6);"
  (insert$ :tbl [:a :b :c] (values [1 2 3] [4 5 6]))
  => "INSERT INTO tbl (a, b, c) VALUES (1, 2, 3), (4, 5, 6);"
  (insert$ :tbl (values [1 2 3] [4 5 6]))
  => "INSERT INTO tbl VALUES (1, 2, 3), (4, 5, 6);"

  ;; single values with or without specifying columns
  (insert$ :tbl (columns :a :b :c) (values [1 2 3]))
  => "INSERT INTO tbl (a, b, c) VALUES (1, 2, 3);"
  (insert$ :tbl (columns :a :b :c) (value 1 2 3))
  => "INSERT INTO tbl (a, b, c) VALUES (1, 2, 3);"
  (insert$ :tbl [:a :b :c] (value 1 2 3))
  => "INSERT INTO tbl (a, b, c) VALUES (1, 2, 3);"
  (insert$ :tbl (value 1 2 3))
  => "INSERT INTO tbl VALUES (1, 2, 3);"

  ;; default values
  (insert$ :tbl (default-values))
  => "INSERT INTO tbl DEFAULT VALUES;"
  (insert$ :tbl (defaults))
  => "INSERT INTO tbl DEFAULT VALUES;"

  ;; insert via SELECT
  (insert$ :tbl (columns :a :b) (select :* (from :otherTbl)))
  => "INSERT INTO tbl (a, b) SELECT * FROM otherTbl;"
  (insert$ :tbl [:a :b] (select :* (from :otherTbl)))
  => "INSERT INTO tbl (a, b) SELECT * FROM otherTbl;"
  (insert$ :tbl (select :* (from :otherTbl)))
  => "INSERT INTO tbl SELECT * FROM otherTbl;"
  (insert$ :tbl (values (select :* (from :otherTbl))))
  => "INSERT INTO tbl SELECT * FROM otherTbl;"

  ;; alternate verbs
  (replace-into$ :tbl (values [1 2 3]))
  => "REPLACE INTO tbl VALUES (1, 2, 3);"
  (insert-or-replace$ :tbl (values [1 2 3]))
  => "INSERT OR REPLACE INTO tbl VALUES (1, 2, 3);"
  (insert-or-rollback$ :tbl (values [1 2 3]))
  => "INSERT OR ROLLBACK INTO tbl VALUES (1, 2, 3);"
  (insert-or-abort$ :tbl (values [1 2 3]))
  => "INSERT OR ABORT INTO tbl VALUES (1, 2, 3);"
  (insert-or-fail$ :tbl (values [1 2 3]))
  => "INSERT OR FAIL INTO tbl VALUES (1, 2, 3);"
  (insert-or-ignore$ :tbl (values [1 2 3]))
  => "INSERT OR IGNORE INTO tbl VALUES (1, 2, 3);"
  )
