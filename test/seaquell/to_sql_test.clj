(ns seaquell.to-sql-test
  (:use midje.sweet
        seaquell.to-sql))

(fact (query-clauses ["select *" "from tbl" "limit 3" "offset 5"] nil) =>
      "select * from tbl limit 3 offset 5")
(fact (query-clauses ["select *" nil "from tbl" nil "limit 3" nil "offset 5"] nil) =>
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
        (query-clauses [-sc- -fc- -wc- -gbc- -hc- -obc- -lc- -oc-] ";") => ...sql...))

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
(fact (field-to-sql {:field -expr- :as -as-}) => "expr AS as"
      (provided (expr-to-sql -expr-) => "expr"
                (alias-to-sql -as-) => "AS as"))

(fact (name-to-sql :-name-) => "-name-"
      (name-to-sql '-name-) => "-name-"
      (name-to-sql "-name-") => "-name-"
      (name-to-sql #"-name-") => "\"-name-\""
      (name-to-sql #"-db-.-tbl-.-col-") => "\"-db-\".\"-tbl-\".\"-col-\""
      (name-to-sql -raw-) => "-raw-"
      (provided (raw? -raw-) => true
                (raw-to-sql -raw-) => "-raw-"))

(fact (alias-to-sql nil) => nil
      (alias-to-sql -as-) => "AS -as-"
      (provided (name-to-sql -as-) => "-as-"))

(fact (from-clause [-j1- -j2- -j3-]) => "FROM j1 j2 j3"
      (provided (join-op-to-sql -j1-) => "j1"
                (join-op-to-sql {:source -j2- :op ","}) => "j2"
                (join-op-to-sql {:source -j3- :op ","}) => "j3"))

(fact (join-op-to-sql -name-) => "name"
      (provided (name-to-sql -name-) => "name"))
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
              (name-to-sql -u1-) => "-u1-"
              (name-to-sql -u2-) => "-u2-")))

(fact
  (let [src {:source -src- :indexed-by -ix-}]
    (join-src-to-sql src) => "src INDEXED BY ix"
    (provided (name-to-sql -src-) => "src"
              (name-to-sql -ix-) => "ix")))

(fact
  (let [src {:source -src- :indexed-by nil}]
    (join-src-to-sql src) => "src NOT INDEXED"
    (provided (name-to-sql -src-) => "src")))

(facts
  (join-src-to-sql {:source {:sql-stmt :select}}) => "(subselect)"
  (provided (to-sql {:sql-stmt :select} false) => "subselect")

  (join-src-to-sql {:source {:sql-stmt :select}, :as -as-}) =>
  "(subselect) AS -as-"
  (provided (to-sql {:sql-stmt :select} false) => "subselect"
            (alias-to-sql -as-) => "AS -as-")

  (join-src-to-sql {:source [-j1- -j2- -j3-]}) => "(j1 j2 j3)"
  (provided (join-op-to-sql -j1-) => "j1"
            (join-op-to-sql -j2-) => "j2"
            (join-op-to-sql -j3-) => "j3")

  (join-src-to-sql {:source -src-}) => "src"
  (provided (name-to-sql -src-) => "src")

  (join-src-to-sql {:source -src- :as -as-}) => "src AS -as-"
  (provided (name-to-sql -src-) => "src"
            (alias-to-sql -as-) => "AS -as-"))

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

(facts (expr-to-sql* -prec- nil) => "NULL"
       (expr-to-sql* -prec- 78) => "78"
       (expr-to-sql* -prec- 78.9) => "78.9"
       (expr-to-sql* -prec- true) => "TRUE"
       (expr-to-sql* -prec- false) => "FALSE"
       (expr-to-sql* -prec- "any string") => "'any string'"
       (expr-to-sql* -prec- \c) => "'c'"
       (expr-to-sql* -prec- :kw) => "kw"
       (provided (name-to-sql :kw) => "kw")
       (expr-to-sql* -prec- #"-name-") => -quoted-
       (provided (name-to-sql #"-name-") => -quoted-)
       (expr-to-sql* -prec- -raw-) => "-raw-"
       (provided (raw? -raw-) => true
                 (raw-to-sql -raw-) => "-raw-")
       (expr-to-sql* -prec- {:binary "abc"}) => "x'0abc'"
       (expr-to-sql* -prec- {:binary "abcd"}) => "x'abcd'"
       (expr-to-sql* -prec- {:interval -e1- :units -u-}) => "INTERVAL -e1- -u-"
       (provided (interval-to-sql {:interval -e1- :units -u-})
                 => "INTERVAL -e1- -u-")
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
         (expr-to-sql* -prec- [range -a1- -a2-]) => "(-a1-, -a2-)"
         (provided (fn-call-to-sql "" [-a1- -a2-]) => "(-a1-, -a2-)"))
       (fact
         (expr-to-sql* -prec- [val -a1- -a2-]) => "(-a1-, -a2-)"
         (provided (fn-call-to-sql "" [-a1- -a2-]) => "(-a1-, -a2-)"))
       (fact
         (expr-to-sql* -prec- [vals -a1- -a2-]) => "(-a1-, -a2-)"
         (provided (fn-call-to-sql "" [-a1- -a2-]) => "(-a1-, -a2-)"))
       (fact
         (expr-to-sql* -prec- [-bin-op- -a1- -a2-]) => -expr-
         (provided (normalize-fn-or-op -bin-op-) => :-op-
                   (arith-bin-ops :-op-) => :-op-
                   (bin-op-to-sql -prec- :-op- [-a1- -a2-]) => -expr-))
       (fact
         (expr-to-sql* -prec- [-rel-op- -a1- -a2-]) => -expr-
         (provided (normalize-fn-or-op -rel-op-) => :-op-
                   (rel-bin-ops :-op-) => :-op-
                   (rel-op-to-sql -prec- :-op- [-a1- -a2-]) => -expr-))
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

(fact (fn-call-to-sql "FN" [:-a1-]) => "FN(a1)"
      (provided (expr-to-sql :-a1-) => "a1"))

(fact (fn-call-to-sql "FN" [:-a1- -a2-]) => "FN(a1, a2)"
      (provided (expr-to-sql :-a1-) => "a1"
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
                (expr-to-sql -type-) => "type"))

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

(facts (raw-to-sql {:raw :keyword}) => "keyword"
       (raw-to-sql {:raw -raw-}) => "-raw-")

(fact (interval-to-sql {:interval -e1- :units -units-}) => "INTERVAL -e1- -units-"
      (provided (expr-to-sql -e1-) => "-e1-"
                (to-sql-keywords -units-) => "-units-"))

(facts
  (map-to-expr {-e1- -e2-}) => [:= -e1- -e2-]
  (map-to-expr {-e1- -e2- -e3- -e4-}) => [:and [:= -e1- -e2-] [:= -e3- -e4-]]
  (map-to-expr {-e1- [..op.. -e2-]}) => [:= -e1- [..op.. -e2-]]
  (provided (predicate? "..OP..") => false)
  (map-to-expr {-e1- [..op.. -e2-]}) => ["..OP.." -e1- -e2-]
  (provided (predicate? "..OP..") => true))

(facts (to-sql-keywords "any string") => "any string"
       (to-sql-keywords :left-outer-join) => "LEFT OUTER JOIN"
       (to-sql-keywords -raw-) => "-raw-"
       (provided (raw? -raw-) => true
                 (raw-to-sql -raw-) => "-raw-"))

(fact
  (let [stmt {:sql-stmt :delete :source -src- :indexed-by -ix-
              :where -w- :order-by -ob- :limit -l- :offset -o-}]
    (to-sql stmt)
      => ...sql...
      (provided
        (join-src-to-sql stmt) => -src-
        (where-clause -w-) => -wc-
        (order-by-clause -ob-) => -obc-
        (limit-clause -l-) => -lc-
        (offset-clause -o-) => -oc-
        (query-clauses ["DELETE FROM -src-" -wc- -obc- -lc- -oc-] ";") => ...sql...)))

(fact
  (let [stmt {:sql-stmt :insert :source -src- :op -op-
              :columns -cs- :values -vs-}]
    (to-sql stmt)
      => ...sql...
      (provided
        (to-sql-keywords -op-) => "INSERT"
        (expr-to-sql -src-) => "src"
        (columns-to-sql -cs-) => -cols-
        (values-to-sql -vs-) => -vals-
        (query-clauses ["INSERT INTO src" -cols- -vals-] ";") => ...sql...)))

(fact
  (let [stmt {:sql-stmt :update :source -src- :indexed-by -ix- :op -op-
              :set-cols -s- :where -w- :order-by -ob- :limit -l- :offset -o-}]
    (to-sql stmt)
      => ...sql...
      (provided
        (to-sql-keywords -op-) => "UPDATE"
        (join-src-to-sql stmt) => -src-
        (set-clause -s-) => -sc-
        (where-clause -w-) => -wc-
        (order-by-clause -ob-) => -obc-
        (limit-clause -l-) => -lc-
        (offset-clause -o-) => -oc-
        (query-clauses ["UPDATE -src-" -sc- -wc- -obc- -lc- -oc-] ";") =>
        ...sql...)))

