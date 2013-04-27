(ns seaquell.core-test
  (:use midje.sweet
        seaquell.core))

(fact (select :fld) => {:sql-stmt :select :fields -fs-}
      (provided (fields* [] [:fld]) => -fs-))

(fact (select [-f1- -f2- -f3-]) => {:sql-stmt :select :fields -fs-}
      (provided (fields* [] [-f1- -f2- -f3-]) => -fs-))

(fact (as -alias-) => {:as -alias-})

(facts
  (field -f-) => {:field -f-}
  (field -f- -as-) => {:field -f- :as -as-}
  (field -f- :as -as-) => {:field -f- :as -as-}
  (field -f- :bad-as -as-) => (throws AssertionError))

(facts
  (fields) => {:fields []}
  (fields -f-) => {:fields [-f-]}
  (fields -f1- :field2 :as :f2) => {:fields [-f1- {:field :field2 :as :f2}]}
  (fields -f1- :field2 (as :f2)) => {:fields [-f1- {:field :field2 :as :f2}]}
  (fields -f1- (field :field2) (as :f2)) => {:fields [-f1- {:field :field2 :as :f2}]}
  (fields -f1- (field :field2 :as :old-f2) (as :f2) -f3-) =>
  {:fields [-f1- {:field :field2 :as :f2} -f3-]})

(fact "to-sql throws for unsupported statements"
      (to-sql nil) => (throws RuntimeException))

(fact "select does not require a FROM clause"
      (to-sql (select [[* 7 6]])) => "SELECT 7 * 6;")

(def q (select :* (from :user)))

(fact (to-sql q) => "SELECT * FROM user;")

(fact (to-sql (select [:id :passwd] (from :user))) =>
      "SELECT id, passwd FROM user;")

(facts "seaquell supports ALL and DISTINCT modifiers"
  (fact (to-sql (select q (modifier :all))) =>
        "SELECT ALL * FROM user;")
  (fact (to-sql (select q (all))) =>
        "SELECT ALL * FROM user;")
  (fact (to-sql (select-all :* (from :user))) =>
        "SELECT ALL * FROM user;")
  (fact (to-sql (select q (modifier :distinct))) =>
        "SELECT DISTINCT * FROM user;")
  (fact (to-sql (select q (distinkt))) =>
        "SELECT DISTINCT * FROM user;")
  (fact (to-sql (select-distinct :* (from :user))) =>
        "SELECT DISTINCT * FROM user;"))

(facts "seaquell supports LIMIT and OFFSET clauses"
  (fact (to-sql (select q (limit 3))) =>
        "SELECT * FROM user LIMIT 3;")
  (fact (to-sql (select q (offset 4))) =>
        "SELECT * FROM user OFFSET 4;"))

(facts "seaquell supports ORDER BY clause"
  (fact (to-sql (select q (order-by :id))) =>
        "SELECT * FROM user ORDER BY id;")
  (fact (to-sql (select q (order-by :id :passwd))) =>
        "SELECT * FROM user ORDER BY id, passwd;")
  (fact (to-sql (select q (order-by (asc :age :weight) :id :passwd
                                    (desc :height)))) =>
        "SELECT * FROM user ORDER BY age ASC, weight ASC, id, passwd, height DESC;"))

(fact "seaquell supports primitive WHERE clause"
      (to-sql (select q (where [> :id 3]))) =>
      "SELECT * FROM user WHERE id > 3;")

(fact "select handles group clause"
      (-> (select q (group :this :that)) (:group)) => [:this :that])

(fact "select handles having clause"
      (-> (select q (having :expr)) (:having)) => :expr)

(facts "seaquell honors proper order of clauses"
  (fact "LIMIT precedes OFFSET even if they're reversed in (select)"
        (to-sql (select q (offset 4) (limit 3))) =>
        "SELECT * FROM user LIMIT 3 OFFSET 4;")
  (fact "ORDER BY precedes LIMIT even if reversed in (select)"
        (to-sql (select q (limit 3) (order-by :id))) =>
        "SELECT * FROM user ORDER BY id LIMIT 3;"))

(fact "to-sql generates GROUP BY clause"
      (to-sql {:sql-stmt :select
               :fields [{:field :*}]
               :from :user
               :group [:make :model]}) =>
      "SELECT * FROM user GROUP BY make, model;")

(fact "to-sql generates HAVING clause"
      (to-sql {:sql-stmt :select
               :fields [{:field :visits}]
               :from :user
               :group [:visits]
               :having [> :visits 1]
               }) =>
      "SELECT visits FROM user GROUP BY visits HAVING visits > 1;")

(fact "Passing a statement as first arg of select lets you add clauses to it"
      (-> (select q (where [> :num 3])) (to-sql)) =>
      "SELECT * FROM user WHERE num > 3;")

(fact (from -x-) => {:from [-x-]})
(fact (from -x- :y -z-) => {:from [-x- :y -z-]})
(fact (from [-seq-] :rest) => {:from [{:source [-seq-]} :rest]})
(fact (from -x- :as -as-) => {:from [{:source -x- :as -as-}]})
(fact (from -x- (as -as-)) => {:from [{:source -x- :as -as-}]})
(fact (from -x- (as -as-) -rest-) => {:from [{:source -x- :as -as-} -rest-]})

(fact (join -src- (as -a-) (on -expr-)) => {:source -src- :op :join :as -a- :on -expr-})

(fact (mk-join-fns :fancy) =expands-to=>
      (do (clojure.core/defn fancy-join [src & body]
            (diesel.core/mk-map* {:source src :op :fancy-join} body))))

(fact (src -src-) => {:source -src-})
(fact (src -src- (as -as-)) => {:source -src- :as -as-})
(fact (comma-join -src-) => {:source -src- :op ","})
(fact (comma-join -src- (as -as-)) => {:source -src- :op "," :as -as-})

(fact (using -pk-) => {:using [-pk-]})
(fact (using -pk- -pk2-) => {:using [-pk- -pk2-]})

(fact (interval -i- -u-) => {:interval -i- :units -u-})

(fact (raw -raw-) {:raw -raw-})

(fact (limit -lim-) => {:limit -lim-})
(fact (limit -off- -lim-) => {:limit -lim- :offset -off-})
