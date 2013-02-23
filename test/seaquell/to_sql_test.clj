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
(fact (join-op-to-sql {:source -src- :op -op- :on -on-}) =>
      "JOIN src ON expr"
      (provided (to-sql-keywords -op-) => "JOIN"
                (join-src-to-sql -src-) => "src"
                (expr-to-sql -on-) => "expr"))
(fact (join-op-to-sql {:source -src- :op -op- :using [-u1- -u2-]}) =>
      "JOIN src USING (u1, u2)"
      (provided (to-sql-keywords -op-) => "JOIN"
                (join-src-to-sql -src-) => "src"
                (field-to-sql -u1-) => "u1"
                (field-to-sql -u2-) => "u2"))

(facts
  (join-src-to-sql :kw) => "kw"
  (join-src-to-sql "any string") => "any string"
  (join-src-to-sql {:table :db.table}) => "db.table"
  (join-src-to-sql {:table :db.table :as :T}) => "db.table AS T"
  (fact
    (join-src-to-sql {:select -q-}) => "(subselect)"
    (provided (to-sql -q- false) => "subselect"))
  (fact
    (join-src-to-sql {:select -q- :as -as-}) => "(subselect) AS -as-"
    (provided (to-sql -q- false) => "subselect"))
  (fact
    (join-src-to-sql [-j1- -j2- -j3-]) => "(j1 j2 j3)"
    (provided (join-op-to-sql -j1-) => "j1"
              (join-op-to-sql -j2-) => "j2"
              (join-op-to-sql -j3-) => "j3")))

(fact (where-clause nil) => nil)
(fact (where-clause -expr-) => "WHERE expr"
      (provided (expr-to-sql -expr-) => "expr"))

(fact (group-clause nil) => nil)
(fact (group-clause [-expr-]) => "GROUP BY expr"
      (provided (expr-to-sql -expr-) => "expr"))
(fact (group-clause [-ex1- -ex2-]) => "GROUP BY ex1, ex2"
      (provided (expr-to-sql -ex1-) => "ex1"
                (expr-to-sql -ex2-) => "ex2"))

(fact (having-clause nil) => nil)
(fact (having-clause -expr-) => "HAVING expr"
      (provided (expr-to-sql -expr-) => "expr"))

(fact (order-by-clause nil) => nil)
(fact (order-by-clause [-ord-]) => "ORDER BY ord"
      (provided (order-item -ord-) => "ord"))
(fact (order-by-clause [-o1- -o2-]) => "ORDER BY o1, o2"
      (provided (order-item -o1-) => "o1")
      (provided (order-item -o2-) => "o2"))

(fact (limit-clause nil) => nil)
(fact (limit-clause -expr-) => "LIMIT expr"
      (provided (expr-to-sql -expr-) => "expr"))

(fact (offset-clause nil) => nil)
(fact (offset-clause -expr-) => "OFFSET expr"
      (provided (expr-to-sql -expr-) => "expr"))

(fact (order-item -expr-) => "expr"
      (provided (order-item? -expr-) => false)
      (provided (expr-to-sql -expr-) => "expr"))

(fact (order-item {:expr [-expr-]}) => ["expr"]
      (provided (order-item? {:expr [-expr-]}) => true)
      (provided (expr-to-sql -expr-) => "expr"))

(fact (order-item {:expr [-ex1- -ex2-]}) => ["ex1" "ex2"]
      (provided (order-item? {:expr [-ex1- -ex2-]}) => true)
      (provided (expr-to-sql -ex1-) => "ex1")
      (provided (expr-to-sql -ex2-) => "ex2"))

(fact (order-item {:expr [-ex1- -ex2-] :order :asc}) => ["ex1 ASC" "ex2 ASC"]
      (provided (order-item? {:expr [-ex1- -ex2-] :order :asc}) => true)
      (provided (expr-to-sql -ex1-) => "ex1")
      (provided (expr-to-sql -ex2-) => "ex2"))

(fact (order-item {:expr [-ex1- -ex2-] :order :asc :collate -coll-}) =>
      ["ex1 COLLATE -coll- ASC" "ex2 COLLATE -coll- ASC"]
      (provided (order-item? {:expr [-ex1- -ex2-] :order :asc :collate -coll-}) => true)
      (provided (expr-to-sql -ex1-) => "ex1")
      (provided (expr-to-sql -ex2-) => "ex2"))

(facts (expr-to-sql :kw) => "kw"
       (expr-to-sql "any string") => "any string"
       (expr-to-sql {-k1- -v1-}) => "-k1- = -v1-"
       (expr-to-sql {-k1- -v1-, -k2- -v2-}) => "-k1- = -v1- AND -k2- = -v2-")

(facts (field-to-sql :kw) => "kw"
       (field-to-sql "any string") => "any string")

(facts (to-sql-keywords "any string") => "any string"
       (to-sql-keywords :left-outer-join) => "LEFT OUTER JOIN")
