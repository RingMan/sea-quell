(ns sequel.core-test
  (:use midje.sweet
        sequel.core))

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

(fact (to-sql (select [:id :passwd] (from :user))) => "SELECT id, passwd FROM user;")
