(ns seaquell.core-test
  (:use midje.sweet
        seaquell.core))

(fact (select :*) => {:sql-stmt :select :fields [-f-]}
      (provided (field :*) => -f-))

(fact (select [:first :last]) =>
      {:sql-stmt :select
       :fields [-f1- -f2-]}
      (provided
        (field :first) => -f1-
        (field :last) => -f2-))

(fact (as :alias) => {:as :alias})

(fact (to-sql (select :* (from :user))) => "SELECT * FROM user;")

(fact (to-sql (select [:id :passwd] (from :user))) =>
      "SELECT id, passwd FROM user;")

(facts "seaquell supports LIMIT and OFFSET clauses"
  (fact (to-sql (select :* (from :user) (limit 3))) =>
        "SELECT * FROM user LIMIT 3;")
  (fact (to-sql (select :* (from :user) (offset 4))) =>
        "SELECT * FROM user OFFSET 4;"))

(facts "seaquell supports ORDER BY clause"
  (fact (to-sql (select :* (from :user) (order-by :id))) =>
        "SELECT * FROM user ORDER BY id;")
  (fact (to-sql (select :* (from :user) (order-by :id :passwd))) =>
        "SELECT * FROM user ORDER BY id, passwd;")
  (fact (to-sql (select :* (from :user) (order-by
                                          (asc :age :weight) :id :passwd
                                          (desc :height)))) =>
        "SELECT * FROM user ORDER BY age ASC, weight ASC, id, passwd, height DESC;"))

(facts "seaquell honors proper order of clauses"
  (fact "LIMIT precedes OFFSET even if they're reversed in (select)"
        (to-sql (select :* (from :user) (offset 4) (limit 3))) =>
        "SELECT * FROM user LIMIT 3 OFFSET 4;")
  (fact "ORDER BY precedes LIMIT even if reversed in (select)"
        (to-sql (select :* (from :user) (limit 3) (order-by :id))) =>
        "SELECT * FROM user ORDER BY id LIMIT 3;"))
