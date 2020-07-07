(ns seaquell.core-test
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.util :refer [select?]]))

(fact (select :fld) => {:sql-stmt :select :fields -fs-}
      (provided (fields* [] [:fld]) => -fs-))

(fact (select [-f1- -f2- -f3-]) => {:sql-stmt :select :fields -fs-}
      (provided (fields* [] [-f1- -f2- -f3-]) => -fs-))

(fact (as -alias-) => {:as -alias-})

(facts
  (field -f-) => {:field -f-}
  (field -f- (as -as-)) => {:field -f- :as -as-}
  (field -f- :as -as-) => {:field -f- :as -as-}
  (field (field nil)) => {:field nil}
  (field (field false)) => {:field false}
  (field (field -f-)) => {:field -f-})

(facts
  (fields) => {:fields []}
  (fields -f-) => {:fields [-f-]}
  (fields -f1- :field2 :as :f2) => {:fields [-f1- {:field :field2 :as :f2}]}
  (fields -f1- :field2 (as :f2)) => {:fields [-f1- {:field :field2 :as :f2}]}
  (fields -f1- (field :field2) (as :f2)) => {:fields [-f1- {:field :field2 :as :f2}]}
  (fields -f1- (field :field2 :as :old-f2) (as :f2) -f3-) =>
  {:fields [-f1- {:field :field2 :as :f2} -f3-]})

(fact "to-sql throws for unsupported statements"
      (to-sql {:sql-stmt -unsupported-}) => (throws RuntimeException))

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
  (fact (to-sql (select q (distinct))) =>
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
  (fact (to-sql (select q (order-by (asc :age) :id :passwd (desc :height)))) =>
        "SELECT * FROM user ORDER BY age ASC, id, passwd, height DESC;")
  (fact (to-sql (select q (order-by (asc :id (collate :nocase) (nulls :first))))) =>
        "SELECT * FROM user ORDER BY id COLLATE nocase ASC NULLS FIRST;"))

(fact "seaquell supports primitive WHERE clause"
      (to-sql (select q (where [> :id 3]))) =>
      "SELECT * FROM user WHERE id > 3;")

(fact "select handles group by clause"
      (-> (select q (group-by :this :that)) (:group-by)) => [:this :that])

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
               :group-by [:make :model]}) =>
      "SELECT * FROM user GROUP BY make, model;")

(fact "to-sql generates HAVING clause"
      (to-sql {:sql-stmt :select
               :fields [{:field :visits}]
               :from :user
               :group-by [:visits]
               :having [> :visits 1]
               }) =>
      "SELECT visits FROM user GROUP BY visits HAVING visits > 1;")

(fact "Passing a statement as first arg of select lets you add clauses to it"
      (-> (select q (where [> :num 3])) (to-sql)) =>
      "SELECT * FROM user WHERE num > 3;")

;;; INSERT statements

(facts
  (value -q-) => {:values -q-} (provided (select? -q-) => true)
  (value :default) => {:values :default}
  (let [_c1_ 1, _c2_ 2] (value _c1_ _c2_) => {:values [[_c1_ _c2_]]})

  (values -q-) => {:values -q-} (provided (select? -q-) => true)
  (values :default) => {:values :default}
  (let [_v1_ [1 2], _v2_ [3 4]]
    (values _v1_) => {:values [_v1_]}
    (values _v1_ _v2_) => {:values [_v1_ _v2_]}))

(facts
  (let [q (select :* (from :t))
        _tbl_ :_tbl_
        stmt {:sql-stmt :insert, :op :insert, :into _tbl_}]
    (insert _tbl_ (default-values)) =>
    (merge stmt {:columns nil, :values :default})

    (insert _tbl_ (defaults)) =>
    (merge stmt {:columns nil, :values :default})

    (insert _tbl_ (values :default)) =>
    (merge stmt {:values :default})

    (insert _tbl_ (values q)) =>
    (merge stmt {:values q})

    (let [_v1_ [1 2], _v2_ [3 4]]
      (insert _tbl_ (value _v1_ _v2_)) =>
      (merge stmt {:values [[_v1_ _v2_]]}))

    (insert _tbl_ (values [-a1- -a2-] [-b1- -b2-])) =>
    (merge stmt {:values [[-a1- -a2-] [-b1- -b2-]]})

    (insert _tbl_ (columns -c1- -c2-) (values [-a1- -a2-] [-b1- -b2-])) =>
    (merge stmt {:columns [{:column -c1-} {:column -c2-}]
                 :values [[-a1- -a2-] [-b1- -b2-]]}))

  (let [_tbl_ :_tbl_
        stmt {:sql-stmt :insert, :op :insert, :into _tbl_,
              :columns nil, :values :default}]
    (replace-into _tbl_ (defaults)) => (merge stmt {:op :replace})
    (insert-or-rollback _tbl_ (defaults)) => (merge stmt {:op :insert-or-rollback})
    (insert-or-abort _tbl_ (defaults)) => (merge stmt {:op :insert-or-abort})
    (insert-or-replace _tbl_ (defaults)) => (merge stmt {:op :insert-or-replace})
    (insert-or-fail _tbl_ (defaults)) => (merge stmt {:op :insert-or-fail})
    (insert-or-ignore _tbl_ (defaults)) => (merge stmt {:op :insert-or-ignore})))

;;; UPDATE statements

(facts
  (let [stmt {:sql-stmt :update :op :update :source -tbl-}]
    (update -tbl- (set :-c1- :-v1-)) =>
    (merge stmt {:set {:-c1- :-v1-}})

    (update -tbl- (not-indexed) (set :-c1- :-v1-)) =>
    (merge stmt {:indexed-by nil, :set {:-c1- :-v1-}})

    (update -tbl- (indexed-by -ix-) (set :-c1- :-v1-) (where -expr-)
            (order-by -ord-) (limit -lim-) (offset -off-)) =>
    (merge stmt
           {:indexed-by -ix-
            :set {:-c1- :-v1-}
            :where -expr-
            :order-by [-ord-]
            :limit -lim-
            :offset -off-})))

;;; DELETE statements

(fact (delete :_tbl_) => {:sql-stmt :delete, :from :_tbl_})
(fact (delete :_tbl_ (as -as-)) => {:sql-stmt :delete, :from :_tbl_, :as -as-})
(fact (delete :_tbl_ (as -as-) (indexed-by -ix-)) =>
      {:sql-stmt :delete, :from :_tbl_, :as -as- :indexed-by -ix-})
(fact (delete :_tbl_ (as -as-) (not-indexed)) =>
      {:sql-stmt :delete, :from :_tbl_, :as -as- :indexed-by nil})
(fact (delete :_tbl_ (as -as-) (indexed-by -ix-) (where -where-)
              (order-by -ord-) (limit -lim-) (offset -off-)) =>
      {:sql-stmt :delete
       :from :_tbl_
       :as -as-
       :indexed-by -ix-
       :where -where-
       :order-by [-ord-]
       :limit -lim-
       :offset -off-})

;;; EXPLAIN statements

(fact (explain -stmt-) => {:sql-stmt :explain, :statement -stmt-})
(fact (explain-query-plan -stmt-) =>
      {:sql-stmt :explain-query-plan, :statement -stmt-})

(fact "EXPLAIN is idempotent"
      (let [ex (explain -stmt-)]
        (explain ex) => ex)
      (let [ex (explain-query-plan -stmt-)]
        (explain-query-plan ex) => ex))

;;; Clauses

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

;;; Fragments

(fact
  "cte fn lets you define common table entities with or without column names"
  (let [cte-no-cols {:cte -tbl- :as -q-}
        cte-with-cols {:cte -tbl- :columns [-c1- -c2-] :as -q-}
        cte-with-col-maps {:cte -tbl- :columns [{:column -c1-} {:column -c2-}] :as -q-}
        ]
    (cte -tbl- (as -q-)) => cte-no-cols
    (cte -tbl- :as -q-) => cte-no-cols
    (cte -tbl- (columns -c1- -c2-) (as -q-)) => cte-with-col-maps
    (cte -tbl- (columns -c1- -c2-) :as -q-) => cte-with-col-maps
    (cte -tbl- :columns [-c1- -c2-] (as -q-)) => cte-with-cols
    (cte -tbl- :columns [-c1- -c2-] :as -q-) => cte-with-cols
    (cte -tbl- [-c1- -c2-] (as -q-)) => cte-with-cols
    (cte -tbl- [-c1- -c2-] :as -q-) => cte-with-cols))
