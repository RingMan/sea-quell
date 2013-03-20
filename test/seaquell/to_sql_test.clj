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

(fact (select-clause -mod- -flds-) => "SELECT mod flds"
      (provided (modifier-to-sql -mod-) => "mod ")
      (provided (fields-to-sql -flds-) => "flds"))

(fact (fields-to-sql nil) => "*")
(fact (fields-to-sql :one-fld) => "fld"
      (provided (field-to-sql :one-fld) => "fld"))
(fact (fields-to-sql {:field -f1-}) => "fld"
      (provided (field-to-sql {:field -f1-}) => "fld"))
(fact (fields-to-sql [-f1-]) => "f1"
      (provided (field-to-sql -f1-) => "f1"))
(fact (fields-to-sql [-f1- -f2-]) => "f1, f2"
      (provided (field-to-sql -f1-) => "f1"
                (field-to-sql -f2-) => "f2"))

(fact (field-to-sql -expr-) => "expr"
      (provided (expr-to-sql -expr-) => "expr"))
(fact (field-to-sql {:field -expr-}) => "expr"
      (provided (expr-to-sql -expr-) => "expr"))
(fact (field-to-sql {:field -expr- :as -as-}) => "expr AS -as-"
      (provided (expr-to-sql -expr-) => "expr"))

(fact (from-clause [-j1- -j2- -j3-]) => "FROM j1 j2 j3"
      (provided (join-op-to-sql -j1-) => "j1"
                (join-op-to-sql -j2-) => "j2"
                (join-op-to-sql -j3-) => "j3"))

(fact (join-op-to-sql :db.tbl) => "db.tbl")
(fact (join-op-to-sql {:sql-stmt :select}) => "(qry)"
      (provided (to-sql {:sql-stmt :select} false) => "qry"))
(fact
  (let [jop {:source -src- :op -op- :on -on-}]
    (join-op-to-sql jop) => "JOIN src ON expr"
    (provided (to-sql-keywords -op-) => "JOIN"
              (join-src-to-sql jop) => "src"
              (expr-to-sql -on-) => "expr")))
(fact
  (let [jop {:source -src- :op -op- :using [-u1- -u2-]}]
    (join-op-to-sql jop) => "JOIN src USING (-u1-, -u2-)"
    (provided (to-sql-keywords -op-) => "JOIN"
              (join-src-to-sql jop) => "src"
              (name -u1-) => "-u1-"
              (name -u2-) => "-u2-")))

(facts
  (join-src-to-sql {:source "any string"}) => "any string"
  (join-src-to-sql {:source "any string" :as -as-}) => "any string AS -as-"
  (join-src-to-sql {:source :db.table}) => "db.table"
  (join-src-to-sql {:source :db.table :as -as-}) => "db.table AS -as-"
  (fact
    (join-src-to-sql {:source {:sql-stmt :select}}) => "(subselect)"
    (provided (to-sql {:sql-stmt :select} false) => "subselect"))
  (fact
    (join-src-to-sql {:source {:sql-stmt :select}, :as -as-}) => "(subselect) AS -as-"
    (provided (to-sql {:sql-stmt :select} false) => "subselect"))
  (fact
    (join-src-to-sql {:source [-j1- -j2- -j3-]}) => "(j1 j2 j3)"
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

(facts (expr-to-sql* -prec- nil) => "NULL"
       (expr-to-sql* -prec- 78) => "78"
       (expr-to-sql* -prec- 78.9) => "78.9"
       (expr-to-sql* -prec- true) => "TRUE"
       (expr-to-sql* -prec- false) => "FALSE"
       (expr-to-sql* -prec- :kw) => "kw"
       (expr-to-sql* -prec- "any string") => "'any string'"
       (expr-to-sql* (dec (precedence "AND")) {:k1 :v1}) => "k1 = v1"
       (expr-to-sql* (dec (precedence "AND")) {:k1 :v1, :k2 :v2}) =>
       "k1 = v1 AND k2 = v2"
       (let [q {:sql-stmt :select}]
         (fact (expr-to-sql* -prec- q) => "(sql)"
               (provided (to-sql q false) => "sql")))
       (fact
         (expr-to-sql* -prec- [:func -a1- -a2-]) => -fn-call-
         (provided (fn-call-to-sql "FUNC" [-a1- -a2-]) => -fn-call-))
       (fact
         (expr-to-sql* -prec- [val -a1- -a2-]) => "(-a1-, -a2-)"
         (provided (fn-call-to-sql "" [-a1- -a2-]) => "(-a1-, -a2-)"))
       (fact
         (expr-to-sql* -prec- [vals -a1- -a2-]) => "(-a1-, -a2-)"
         (provided (fn-call-to-sql "" [-a1- -a2-]) => "(-a1-, -a2-)"))
       (fact
         (expr-to-sql* -prec- [-bin-op- -a1- -a2-]) => -expr-
         (provided (normalize-fn-or-op -bin-op-) => -op-
                   (unary-ops -op-) => nil
                   (arith-bin-ops -op-) => -op-
                   (bin-op-to-sql -prec- -op- [-a1- -a2-]) => -expr-))
       (fact
         (expr-to-sql* -prec- [-rel-op- -a1- -a2-]) => -expr-
         (provided (normalize-fn-or-op -rel-op-) => -op-
                   (arith-bin-ops -op-) => nil
                   (rel-bin-ops -op-) => -op-
                   (rel-op-to-sql -prec- -op- [-a1- -a2-]) => -expr-))
       (fact
         (expr-to-sql* -prec- [:between -e1- -e2- -e3-]) => -between-
         (provided (between-to-sql -prec- "BETWEEN" [-e1- -e2- -e3-]) => -between-))
       (fact
         (expr-to-sql* -prec- [:cast -e1- -type-]) => -cast-
         (provided (cast-to-sql -e1- -type-) => -cast-))
       (fact
         (expr-to-sql* -prec- [:case -v- -v1- -e1-]) => -case-
         (provided (case-to-sql -v- [-v1- -e1-]) => -case-))
       (fact
         (expr-to-sql* -prec- [:cond -p1- -e1-]) => -case-
         (provided (cond-to-sql [-p1- -e1-]) => -case-)))

(facts "about unary ops"
       (expr-to-sql* -prec- [-un-op- -e1-]) => "-op- -e1-"
       (provided (normalize-fn-or-op -un-op-) => -op-
                 (unary-ops -op-) => -op-
                 (unary-op-to-sql -op- -e1-) => "-op- -e1-"))

(fact (unary-op-to-sql "NOT" -e1-) => "NOT -e1-"
      (provided (expr-to-sql* unary-prec -e1-) => "-e1-"))

(fact (fn-call-to-sql "FN" [-a1-]) => "FN(a1)"
      (provided (expr-to-sql -a1-) => "a1"))

(fact (fn-call-to-sql "FN" [-a1- -a2-]) => "FN(a1, a2)"
      (provided (expr-to-sql -a1-) => "a1"
                (expr-to-sql -a2-) => "a2"))

(fact (fn-call-to-sql "FN" [distinct -a1-]) => "FN(DISTINCT a1)"
      (provided (expr-to-sql -a1-) => "a1"))

(fact (between-to-sql -1 "BETWEEN" [-e1- -e2- -e3-]) => "e1 BETWEEN e2 AND e3"
      (provided (expr-to-sql* 100 -e1-) => "e1"
                (expr-to-sql* 100 -e2-) => "e2"
                (expr-to-sql* 100 -e3-) => "e3"))

(fact (between-to-sql (inc (precedence "BETWEEN")) "BETWEEN" [-e1- -e2- -e3-])
      => "(e1 BETWEEN e2 AND e3)"
      (provided (expr-to-sql* 100 -e1-) => "e1"
                (expr-to-sql* 100 -e2-) => "e2"
                (expr-to-sql* 100 -e3-) => "e3"))

(fact (cast-to-sql -ex- -type-) => "CAST(ex AS type)"
      (provided (expr-to-sql -ex-) => "ex"
                (to-sql-keywords -type-) => "type"))

(fact (case-to-sql -v- [-v1- -e1-]) => "CASE v WHEN v1 THEN e1 END"
      (provided (expr-to-sql -v-) => "v"
                (cases-to-sql [-v1- -e1-]) => "WHEN v1 THEN e1"))

(fact (case-to-sql -v- [-v1- -e1- -else-]) => "CASE v WHEN v1 THEN e1 ELSE e2 END"
      (provided (expr-to-sql -v-) => "v"
                (expr-to-sql -else-) => "e2"
                (cases-to-sql [-v1- -e1-]) => "WHEN v1 THEN e1"))

(fact (cond-to-sql [-p1- -e1-]) => "CASE WHEN p1 THEN e1 END"
      (provided (cases-to-sql [-p1- -e1-]) => "WHEN p1 THEN e1"))

(fact (cond-to-sql [-p1- -e1- :else -e2-]) => "CASE WHEN p1 THEN e1 ELSE e2 END"
      (provided (cases-to-sql [-p1- -e1- :else -e2-]) => "WHEN p1 THEN e1 ELSE e2"))

(fact (cases-to-sql [-v1- -e1-]) => "WHEN v1 THEN e1"
      (provided (expr-to-sql -v1-) => "v1"
                (expr-to-sql -e1-) => "e1"))

(fact (cases-to-sql [-v1- -e1- -v2- -e2-]) => "WHEN v1 THEN e1 WHEN v2 THEN e2"
      (provided (expr-to-sql -v1-) => "v1"
                (expr-to-sql -e1-) => "e1"
                (expr-to-sql -v2-) => "v2"
                (expr-to-sql -e2-) => "e2"))

(fact (cases-to-sql [-p1- -e1- :else -e2-]) => "WHEN p1 THEN e1 ELSE e2"
      (provided (expr-to-sql -p1-) => "p1"
                (expr-to-sql -e1-) => "e1"
                (expr-to-sql -e2-) => "e2"))

(facts (to-sql-keywords "any string") => "any string"
       (to-sql-keywords :left-outer-join) => "LEFT OUTER JOIN")

