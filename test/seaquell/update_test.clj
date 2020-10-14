(ns seaquell.update-test
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
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
  => "UPDATE OR IGNORE tbl SET a=1, b=2 WHERE c > 5;")

(fact "update is idempotent"
  (update (update :tbl (set {:a 1 :b 2}) (where {:c [> 5]})))
  => (update :tbl (set {:a 1 :b 2}) (where {:c [> 5]})))

(fact "update supports FROM clause as documented at
      https://www.sqlite.org/lang_update.html#update_from"
  (update$ :inventory
           (set {:quantity '(- quantity daily.amt)})
           (from (select ['(sum quantity) (as :amt) :itemId]
                         (from :sales) (group-by 2))
                 (as :daily))
           (where {:inventory.itemId :daily.itemId})) =>
  (str
    "UPDATE inventory SET quantity=quantity - daily.amt "
    "FROM (SELECT SUM(quantity) AS amt, itemId FROM sales GROUP BY 2) AS daily "
    "WHERE inventory.itemId = daily.itemId;"))
