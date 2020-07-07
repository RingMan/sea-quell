(ns seaquell.pragma-test
  "Tests the SQLite PRAGMA command"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.engine :refer [db-conn executes?]]
            [seaquell.sqlite :refer [db-spec]]))

(fact "Seaquell supports the SQLite PRAGMA statement"
  (pragma$ :main.cache_size) => "PRAGMA main.cache_size;"
  (pragma$ :cache_size) => "PRAGMA cache_size;"
  (pragma$ :main.cache_size -2000) => "PRAGMA main.cache_size = -2000;"
  (pragma$ :cache_size -2000) => "PRAGMA cache_size = -2000;"
  (pragma$ [= :cache_size -2000]) => "PRAGMA cache_size = -2000;"
  (pragma$ :cache_size (expr -2000)) => "PRAGMA cache_size = -2000;"
  (pragma$ (expr [= :cache_size -2000])) => "PRAGMA cache_size = -2000;"
  (pragma$ [:main.cache_size -2000]) => "PRAGMA MAIN.CACHE_SIZE(-2000);"
  (pragma$ (expr [:main.cache_size -2000])) => "PRAGMA MAIN.CACHE_SIZE(-2000);")

(let [p :cache_size
      v -2100
      w (- v 100)
      c (db-conn (db-spec))]
  (fact "Can fetch value of a PRAGMA"
        (pragma! p (db c) {:result-set-fn #(-> % first p)}) => number?
        (sql! '[create table t(x) \;] (db c)) => executes?
        (pragma! [:main.table_info :t] (db c) {:row-fn :name}) => ["x"])
  (fact "Can set value of a PRAGMA"
        (pragma! p v (db c)) => executes?
        (pragma! p (db c) {:row-fn p}) => [v]
        (pragma! [= p w] (db c)) => executes?
        (pragma! p (db c) {:row-fn p}) => [w]
        (pragma! [p v] (db c) {:jdbc/query? false}) => executes?
        (pragma! p (db c) {:row-fn p}) => [v])
  (fact "pragma is idempotent"
        (pragma (pragma p v)) => (pragma p v)))

