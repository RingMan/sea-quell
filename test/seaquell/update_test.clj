(ns seaquell.update-test
  (:refer-clojure :exclude [update])
  (:use midje.sweet
        seaquell.core))

(facts "about update"
  ;; simple update
  (update$ :tbl (set-columns {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE tbl SET a=1, b=2 WHERE c > 5;"
  (update$ :tbl (set-cols {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE tbl SET a=1, b=2 WHERE c > 5;"

  ;; using INDEXED BY
  (update$ :tbl (not-indexed) (set-cols {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE tbl NOT INDEXED SET a=1, b=2 WHERE c > 5;"
  (update$ :tbl (indexed-by :ix) (set-cols {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE tbl INDEXED BY ix SET a=1, b=2 WHERE c > 5;"
  (update$ :tbl :indexed-by :ix :set-cols {:a 1 :b 2} :where {:c [> 5]})
  => "UPDATE tbl INDEXED BY ix SET a=1, b=2 WHERE c > 5;"
  
  ;; alternate verbs
  (update-or-rollback$ :tbl (set-cols {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR ROLLBACK tbl SET a=1, b=2 WHERE c > 5;"
  (update-or-abort$ :tbl (set-cols {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR ABORT tbl SET a=1, b=2 WHERE c > 5;"
  (update-or-replace$ :tbl (set-cols {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR REPLACE tbl SET a=1, b=2 WHERE c > 5;"
  (update-or-fail$ :tbl (set-cols {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR FAIL tbl SET a=1, b=2 WHERE c > 5;"
  (update-or-ignore$ :tbl (set-cols {:a 1 :b 2}) (where {:c [> 5]}))
  => "UPDATE OR IGNORE tbl SET a=1, b=2 WHERE c > 5;"
  )
