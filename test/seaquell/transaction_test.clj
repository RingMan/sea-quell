(ns seaquell.transaction-test
  "Tests SQLite BEGIN, COMMIT (END), ROLLBACK, SAVEPOINT and RELEASE commands"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]))

(fact "Seaquell supports the BEGIN statement"
  (fact "begin and its cousins are idempotent"
    (begin (begin)) => (begin))
  (fact "You can begin a transaction"
    (begin$) => "BEGIN;"
    (begin-transaction$) => "BEGIN TRANSACTION;")
  (fact "You can begin a deferred transaction"
    (begin-deferred$) => "BEGIN DEFERRED;"
    (begin-deferred-transaction$) => "BEGIN DEFERRED TRANSACTION;")
  (fact "You can begin an exclusive transaction"
    (begin-exclusive$) => "BEGIN EXCLUSIVE;"
    (begin-exclusive-transaction$) => "BEGIN EXCLUSIVE TRANSACTION;")
  (fact "You can begin an immediate transaction"
    (begin-immediate$) => "BEGIN IMMEDIATE;"
    (begin-immediate-transaction$) => "BEGIN IMMEDIATE TRANSACTION;"))

(fact "Seaquell supports the COMMIT (END) statement"
  (fact "commit and its cousins are idempotent"
    (commit (commit)) => (commit)
    (end (end)) => (end))
  (fact "You can commit a transaction"
    (commit$) => "COMMIT;"
    (commit-transaction$) => "COMMIT TRANSACTION;")
  (fact "You can use end instead of commit"
    (end$) => "END;"
    (end-transaction$) => "END TRANSACTION;"))

(fact "Seaquell suports the ROLLBACK statement"
  (fact "rollback and its cousins are idempotent"
    (rollback (rollback)) => (rollback)
    (rollback-to (rollback-to :sp)) => (rollback-to :sp))
  (fact "You can rollback a transaction"
    (rollback$) => "ROLLBACK;"
    (rollback-transaction$) => "ROLLBACK TRANSACTION;")
  (fact "You can rollback a transaction to a savepoint"
    (rollback-to$ :sp) => "ROLLBACK TO sp;"
    (rollback$ (to :sp)) => "ROLLBACK TO sp;"
    (rollback-to-savepoint$ :sp) => "ROLLBACK TO SAVEPOINT sp;"
    (rollback$ (to-savepoint :sp)) => "ROLLBACK TO SAVEPOINT sp;"
    (rollback-transaction-to$ :sp) => "ROLLBACK TRANSACTION TO sp;"
    (rollback-transaction$ (to :sp)) => "ROLLBACK TRANSACTION TO sp;"
    (rollback-transaction-to-savepoint$ :sp) => "ROLLBACK TRANSACTION TO SAVEPOINT sp;"
    (rollback-transaction$ (to-savepoint :sp)) => "ROLLBACK TRANSACTION TO SAVEPOINT sp;"))

(fact "Seaquell supports the SAVEPOINT statement"
  (fact "savepoint is idempotent"
    (savepoint (savepoint :sp)) => (savepoint :sp))
  (fact "You can create a named savepoint"
    (savepoint$ :sp) => "SAVEPOINT sp;"))

(fact "Seaquell supports the RELEASE statement"
  (fact "release is idempotent"
    (release (release :sp)) => (release :sp))
  (fact "You can release a savepoint"
    (release$ :sp) => "RELEASE sp;"
    (release-savepoint$ :sp) => "RELEASE SAVEPOINT sp;"))

