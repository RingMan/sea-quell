(ns seaquell.delete-test
  (:refer-clojure :exclude [update])
  (:use midje.sweet
        seaquell.core))

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
