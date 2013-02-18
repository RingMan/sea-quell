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

(fact (to-sql (select :* (from :user) (limit 3))) =>
      "SELECT * FROM user LIMIT 3;")
(fact (to-sql (select :* (from :user) (offset 4))) =>
      "SELECT * FROM user OFFSET 4;")
(fact (to-sql (select :* (from :user) (offset 4) (limit 3))) =>
      "SELECT * FROM user LIMIT 3 OFFSET 4;")

