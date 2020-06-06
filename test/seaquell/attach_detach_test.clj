(ns seaquell.attach-detach-test
  "Tests SQLite ATTACH and DETACH commands"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]))

(fact "Seaquell supports the ATTACH statement"
  (fact "You can attach to a database file"
    (attach$ :my.db (as :db)) => "ATTACH 'my.db' AS db;"
    (attach$ :my.db :as :db) => "ATTACH 'my.db' AS db;"
    (attach-database$ :my.db (as :db)) => "ATTACH DATABASE 'my.db' AS db;"
    (attach-database$ :my.db :as :db) => "ATTACH DATABASE 'my.db' AS db;")
  (fact "attach is idempotent"
    (attach (attach :my.db (as :db))) => (attach :my.db (as :db))))

(fact "Seaquell supports the DETACH statement"
  (fact "You can detach from a database file"
    (detach$ :db) => "DETACH db;"
    (detach-database$ :db) => "DETACH DATABASE db;")
  (fact "detach is idempotent"
    (detach (detach :db)) => (detach :db)))


