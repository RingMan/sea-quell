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

(fact "select does not require a FROM clause"
      (to-sql (select "7*6")) => "SELECT 7*6;")

(fact (to-sql (select :* (from :user))) => "SELECT * FROM user;")

(fact (to-sql (select [:id :passwd] (from :user))) =>
      "SELECT id, passwd FROM user;")

(facts "seaquell supports ALL and DISTINCT modifiers"
  (fact (to-sql (select :* (modifier :all) (from :user))) =>
        "SELECT ALL * FROM user;")
  (fact (to-sql (select :* (all) (from :user))) =>
        "SELECT ALL * FROM user;")
  (fact (to-sql (select-all :* (from :user))) =>
        "SELECT ALL * FROM user;")
  (fact (to-sql (select :* (modifier :distinct) (from :user))) =>
        "SELECT DISTINCT * FROM user;")
  (fact (to-sql (select :* (distinkt) (from :user))) =>
        "SELECT DISTINCT * FROM user;")
  (fact (to-sql (select-distinct :* (from :user))) =>
        "SELECT DISTINCT * FROM user;"))

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

(fact "seaquell supports primitive WHERE clause"
      (to-sql (select :* (from :user) (where "(id > 3)"))) =>
      "SELECT * FROM user WHERE (id > 3);")

(fact "select handles group clause"
      (-> (select :* (group :this :that)) (:group)) => [:this :that])

(fact "select handles having clause"
      (-> (select :* (having :expr)) (:having)) => :expr)

(facts "seaquell honors proper order of clauses"
  (fact "LIMIT precedes OFFSET even if they're reversed in (select)"
        (to-sql (select :* (from :user) (offset 4) (limit 3))) =>
        "SELECT * FROM user LIMIT 3 OFFSET 4;")
  (fact "ORDER BY precedes LIMIT even if reversed in (select)"
        (to-sql (select :* (from :user) (limit 3) (order-by :id))) =>
        "SELECT * FROM user ORDER BY id LIMIT 3;"))

(fact "to-sql generates GROUP BY clause"
      (to-sql {:fields [{:field :*}]
               :from :user
               :group [:make :model]}) =>
      "SELECT * FROM user GROUP BY make, model;")

(fact "to-sql generates HAVING clause"
      (to-sql {:fields [{:field :visits}]
               :from :user
               :group [:visits]
               :having "visits > 1"
               }) =>
      "SELECT visits FROM user GROUP BY visits HAVING visits > 1;")

(def q (select :* (from :user)))

(fact "Passing a statement as first arg of select lets you add clauses to it"
      (-> (select q (where "num > 3")) (to-sql)) =>
      "SELECT * FROM user WHERE num > 3;")
