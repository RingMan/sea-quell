(ns seaquell.update-test
  (:refer-clojure :exclude [distinct drop group-by into set update partition-by])
  (:require [midje.sweet :refer :all]
            [seaquell.core :refer :all]))

(facts "about update"
  ;; simple update
  (update$ :tbl (set {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE tbl SET a=1, b=2 WHERE c > 5;"

  ;; using INDEXED BY
  (update$ :tbl (not-indexed) (set {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE tbl NOT INDEXED SET a=1, b=2 WHERE c > 5;"
  (update$ :tbl (indexed-by :ix) (set {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE tbl INDEXED BY ix SET a=1, b=2 WHERE c > 5;"
  (update$ :tbl :indexed-by :ix :set {:a 1 :b 2} :where {:c [> 5]})
  => "UPDATE tbl INDEXED BY ix SET a=1, b=2 WHERE c > 5;"
  
  ;; alternate verbs
  (update-or-rollback$ :tbl (set {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR ROLLBACK tbl SET a=1, b=2 WHERE c > 5;"
  (update-or-abort$ :tbl (set {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR ABORT tbl SET a=1, b=2 WHERE c > 5;"
  (update-or-replace$ :tbl (set {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR REPLACE tbl SET a=1, b=2 WHERE c > 5;"
  (update-or-fail$ :tbl (set {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR FAIL tbl SET a=1, b=2 WHERE c > 5;"
  (update-or-ignore$ :tbl (set {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR IGNORE tbl SET a=1, b=2 WHERE c > 5;"
  )
