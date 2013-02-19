(ns seaquell.to-sql-test
  (:use midje.sweet
        seaquell.to-sql))

(fact (select-clauses ["select *" "from tbl" "limit 3" "offset 5"] nil) =>
      "select * from tbl limit 3 offset 5")
(fact (select-clauses ["select *" nil "from tbl" nil "limit 3" nil "offset 5"] nil) =>
      "select * from tbl limit 3 offset 5")

(fact (to-sql {:sql-stmt :select
               :modifier -m- :fields -flds- :from -f- :where -w-
               :group -gb- :having -h- :order-by -ob- :limit -l- :offset -o-})
      => ...sql...
      (provided
        (select-clause -m- -flds-) => -sc-
        (from-clause -f-) => -fc-
        (where-clause -w-) => -wc-
        (group-clause -gb-) => -gbc-
        (having-clause -h-) => -hc-
        (order-by-clause -ob-) => -obc-
        (limit-clause -l-) => -lc-
        (offset-clause -o-) => -oc-
        (select-clauses [-sc- -fc- -wc- -gbc- -hc- -obc- -lc- -oc-] ";") => ...sql...))

(fact (from-clause [-j1- -j2- -j3-]) => "FROM j1 j2 j3"
      (provided (join-op-to-sql -j1-) => "j1"
                (join-op-to-sql -j2-) => "j2"
                (join-op-to-sql -j3-) => "j3"))

(fact (join-op-to-sql :db.tbl) => "db.tbl")
