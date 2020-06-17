(ns seaquell.delete-test
  (:refer-clojure :exclude [distinct drop group-by into set update partition-by])
  (:require [midje.sweet :refer :all]
            [seaquell.core :refer :all]))

(facts "about delete"
  ;; simple delete
  (delete$ :db.tbl)
  => "DELETE FROM db.tbl;"

  ;; using WHERE
  (delete$ :tbl (where [> :c 5]))
  => "DELETE FROM tbl WHERE c > 5;"

  ;; using INDEXED BY
  (delete$ :tbl (not-indexed) (where [> :c 5]))
  => "DELETE FROM tbl NOT INDEXED WHERE c > 5;"
  (delete$ :tbl (indexed-by :ix) (where [> :c 5]))
  => "DELETE FROM tbl INDEXED BY ix WHERE c > 5;"
  (delete$ :tbl :indexed-by :ix :where [> :c 5])
  => "DELETE FROM tbl INDEXED BY ix WHERE c > 5;"
  )
