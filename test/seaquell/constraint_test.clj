(ns seaquell.constraint-test
  "Tests that column constraints for CREATE TABLE render properly"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.to-sql :refer
             [constraint-to-sql constraints-to-sql expr-to-sql
              join-by-space on-conflict-clause to-sql-keywords]]))

(fact "You can name a constraint by passing the name as the first arg"
  (constraint-to-sql (constraint :pk :valid-constraint)) =>
  "CONSTRAINT pk VALID CONSTRAINT")

(fact "You don't have to name a constraint, though"
  (constraint-to-sql (constraint :valid-constraint)) => "VALID CONSTRAINT")

(fact "The constraints function forwards args to constraint, if needed"
  (let [->sql #(-> % :constraints (constraints-to-sql join-by-space))]
    (->sql (constraints (constraint (not-null)) (constraint :pk (primary-key)))) =>
    "NOT NULL CONSTRAINT pk PRIMARY KEY"
    (->sql (constraints (not-null) [:pk (primary-key)])) =>
    "NOT NULL CONSTRAINT pk PRIMARY KEY"))

(fact "There are multiple ways to specify an ON CONFLICT clause in a constraint"
  (let [->sql #(-> % :on-conflict on-conflict-clause)]
    (->sql (on-conflict :abort)) => "ON CONFLICT ABORT"
    (->sql (on-conflict :fail)) => "ON CONFLICT FAIL"
    (->sql (on-conflict :ignore)) => "ON CONFLICT IGNORE"
    (->sql (on-conflict :replace)) => "ON CONFLICT REPLACE"
    (->sql (on-conflict :rollback)) => "ON CONFLICT ROLLBACK"
    (->sql (on-conflict-abort)) => "ON CONFLICT ABORT"
    (->sql (on-conflict-fail)) => "ON CONFLICT FAIL"
    (->sql (on-conflict-ignore)) => "ON CONFLICT IGNORE"
    (->sql (on-conflict-replace)) => "ON CONFLICT REPLACE"
    (->sql (on-conflict-rollback)) => "ON CONFLICT ROLLBACK"))

(defn ->sql [c]
  (-> c constraint constraint-to-sql))

(fact "You can specify a PRIMARY KEY column constraint"
  (->sql (primary-key)) => "PRIMARY KEY"
  (->sql (primary-key ASC)) => "PRIMARY KEY ASC"
  (->sql (primary-key (asc))) => "PRIMARY KEY ASC"
  (->sql (primary-key (order :asc))) => "PRIMARY KEY ASC"
  (->sql (primary-key (desc))) => "PRIMARY KEY DESC"
  (->sql (primary-key DESC)) => "PRIMARY KEY DESC"
  (->sql (primary-key (order :desc))) => "PRIMARY KEY DESC"
  (->sql (primary-key (autoincrement))) => "PRIMARY KEY AUTOINCREMENT"
  (->sql (primary-key AUTOINCREMENT)) => "PRIMARY KEY AUTOINCREMENT"
  (->sql (primary-key (on-conflict :fail))) => "PRIMARY KEY ON CONFLICT FAIL"
  (->sql (primary-key ASC (on-conflict :fail) AUTOINCREMENT)) =>
  "PRIMARY KEY ASC ON CONFLICT FAIL AUTOINCREMENT")

(fact "You can specify a PRIMARY KEY table constraint,
      with or without an ON CONFLICT clause"
  (->sql (primary-key (columns (column :a) (column :b)))) =>
  "PRIMARY KEY (a, b)"
  (->sql (primary-key (columns :a :b))) => "PRIMARY KEY (a, b)"
  (->sql (primary-key [:a :b])) => "PRIMARY KEY (a, b)"
  (->sql (primary-key [:a :b] (on-conflict :fail))) =>
  "PRIMARY KEY (a, b) ON CONFLICT FAIL"
  (->sql (primary-key (columns (column :a (collate :nocase) (asc))
                               (column :b (collate :rtrim) (desc))))) =>
  "PRIMARY KEY (a COLLATE nocase ASC, b COLLATE rtrim DESC)"
  (->sql (primary-key (columns [:a (collate :nocase) (asc)]
                               [:b (collate :rtrim) (desc)]))) =>
  "PRIMARY KEY (a COLLATE nocase ASC, b COLLATE rtrim DESC)"
  (->sql (primary-key [[:a (collate :nocase) (asc)]
                       [:b (collate :rtrim) (desc)]])) =>
  "PRIMARY KEY (a COLLATE nocase ASC, b COLLATE rtrim DESC)")

(fact "You can specify a NOT NULL column constraint"
  (->sql (not-null)) => "NOT NULL"
  (->sql (not-null (on-conflict :fail))) => "NOT NULL ON CONFLICT FAIL")

(fact "You can specify a UNIQUE column constraint"
  (->sql (unique)) => "UNIQUE"
  (->sql (unique (on-conflict :fail))) => "UNIQUE ON CONFLICT FAIL")

(fact "You can specify a UNIQUE table constraint,
      with or without an ON CONFLICT clause"
  (->sql (unique (columns (column :a) (column :b)))) =>
  "UNIQUE (a, b)"
  (->sql (unique (columns :a :b))) => "UNIQUE (a, b)"
  (->sql (unique [:a :b])) => "UNIQUE (a, b)"
  (->sql (unique [:a :b] (on-conflict :fail))) =>
  "UNIQUE (a, b) ON CONFLICT FAIL"
  (->sql (unique (columns (column :a (collate :nocase) (asc))
                          (column :b (collate :rtrim) (desc))))) =>
  "UNIQUE (a COLLATE nocase ASC, b COLLATE rtrim DESC)"
  (->sql (unique (columns [:a (collate :nocase) (asc)]
                          [:b (collate :rtrim) (desc)]))) =>
  "UNIQUE (a COLLATE nocase ASC, b COLLATE rtrim DESC)"
  (->sql (unique [[:a (collate :nocase) (asc)]
                  [:b (collate :rtrim) (desc)]])) =>
  "UNIQUE (a COLLATE nocase ASC, b COLLATE rtrim DESC)")

(fact "You can specify a COLLATE column constraint"
  (->sql (collate :collation_name)) => "COLLATE collation_name")

(fact "You can specify a CHECK column or table constraint"
  (->sql (check -expr-)) => "CHECK (-expr-)"
  (provided (expr-to-sql -expr-) => "-expr-"))

(fact "You can specify a DEFAULT column constraint"
  (->sql (default nil)) => "DEFAULT NULL"
  (->sql (default false)) => "DEFAULT FALSE"
  (->sql (default [val -expr-])) => "DEFAULT (-expr-)"
  (provided (expr-to-sql [val -expr-]) => "(-expr-)")
  (->sql (default -expr-)) => "DEFAULT -expr-"
  (provided (expr-to-sql -expr-) => "-expr-"))

(fact "You can specify that a column is GENERATED"
  (->sql (generated (as -expr-))) => "AS (-expr-)"
  (->sql (generated :as -expr-)) => "AS (-expr-)"
  (->sql (generated-as -expr-)) => "AS (-expr-)"
  (->sql (generated (as -expr-) (stored))) => "AS (-expr-) STORED"
  (->sql (generated (as -expr-) (virtual))) => "AS (-expr-) VIRTUAL"
  (->sql (generated (always) (as -expr-))) => "GENERATED ALWAYS AS (-expr-)"
  (->sql (generated (always) (as -expr-) (stored))) =>
  "GENERATED ALWAYS AS (-expr-) STORED"
  (->sql (generated (always) (as -expr-) (virtual))) =>
  "GENERATED ALWAYS AS (-expr-) VIRTUAL"
  (->sql (generated-always (as -expr-))) => "GENERATED ALWAYS AS (-expr-)"
  (->sql (generated-always (as -expr-) (stored))) =>
  "GENERATED ALWAYS AS (-expr-) STORED"
  (->sql (generated-always (as -expr-) (virtual))) =>
  "GENERATED ALWAYS AS (-expr-) VIRTUAL"
  (->sql (generated-always-as -expr-)) => "GENERATED ALWAYS AS (-expr-)"
  (->sql (generated-always-as -expr- (stored))) =>
  "GENERATED ALWAYS AS (-expr-) STORED"
  (->sql (generated-always-as -expr- (virtual))) =>
  "GENERATED ALWAYS AS (-expr-) VIRTUAL"
  (against-background (expr-to-sql -expr-) => "-expr-"))

(facts "About the REFERENCES clause in column and table constraints"
  (->sql (references (table :t))) => "REFERENCES t"
  (->sql (references :t)) => "REFERENCES t"
  (fact "references is idempotent"
    (references (references (table :t))) => (references (table :t)))
  (fact "You can specify which foreign table column(s) to reference"
    (->sql (references :t [:c1])) => "REFERENCES t (c1)"
    (->sql (references :t (columns :c1))) => "REFERENCES t (c1)"
    (->sql (references :t (columns (column :c1)))) => "REFERENCES t (c1)"
    (->sql (references :t [:c1 :c2])) => "REFERENCES t (c1, c2)"
    (->sql (references :t (columns :c1 :c2))) => "REFERENCES t (c1, c2)"
    (->sql (references :t (columns (column :c1) (column :c2)))) =>
    "REFERENCES t (c1, c2)")
  (fact "You can specify an ON DELETE action generically"
    (->sql (references :t (on-delete -action-))) =>
    "REFERENCES t ON DELETE -action-"
    (provided (to-sql-keywords -action-) => "-action-"))
  (fact "You can specify an ON DELETE action specifically"
    (->sql (references :t (on-delete-cascade))) =>
    "REFERENCES t ON DELETE CASCADE"
    (->sql (references :t (on-delete-no-action))) =>
    "REFERENCES t ON DELETE NO ACTION"
    (->sql (references :t (on-delete-restrict))) =>
    "REFERENCES t ON DELETE RESTRICT"
    (->sql (references :t (on-delete-set-default))) =>
    "REFERENCES t ON DELETE SET DEFAULT"
    (->sql (references :t (on-delete-set-null))) =>
    "REFERENCES t ON DELETE SET NULL")
  (fact "You can specify an ON UPDATE action generically"
    (->sql (references :t (on-update -action-))) =>
    "REFERENCES t ON UPDATE -action-"
    (provided (to-sql-keywords -action-) => "-action-"))
  (fact "You can specify an ON UPDATE action specifically"
    (->sql (references :t (on-update-cascade))) =>
    "REFERENCES t ON UPDATE CASCADE"
    (->sql (references :t (on-update-no-action))) =>
    "REFERENCES t ON UPDATE NO ACTION"
    (->sql (references :t (on-update-restrict))) =>
    "REFERENCES t ON UPDATE RESTRICT"
    (->sql (references :t (on-update-set-default))) =>
    "REFERENCES t ON UPDATE SET DEFAULT"
    (->sql (references :t (on-update-set-null))) =>
    "REFERENCES t ON UPDATE SET NULL")
  (fact "You can specify a MATCH clause"
    (->sql (references :t (match -name-))) => "REFERENCES t MATCH (-name-)"
    (provided (to-sql-keywords -name-) => "-name-"))
  (fact "You can say whether foreign key constraints are DEFERRABLE"
    (->sql (references :t (deferrable))) => "REFERENCES t DEFERRABLE"
    (->sql (references :t (deferrable (initially :deferred)))) =>
    "REFERENCES t DEFERRABLE INITIALLY DEFERRED"
    (->sql (references :t (deferrable-initially-deferred))) =>
    "REFERENCES t DEFERRABLE INITIALLY DEFERRED"
    (->sql (references :t (deferrable (initially :immediate)))) =>
    "REFERENCES t DEFERRABLE INITIALLY IMMEDIATE"
    (->sql (references :t (deferrable-initially-immediate))) =>
    "REFERENCES t DEFERRABLE INITIALLY IMMEDIATE"
    (->sql (references :t (not-deferrable))) => "REFERENCES t NOT DEFERRABLE"
    (->sql (references :t (deferrable (modifier :not)))) => "REFERENCES t NOT DEFERRABLE"
    (->sql (references :t (not-deferrable (initially :deferred)))) =>
    "REFERENCES t NOT DEFERRABLE INITIALLY DEFERRED"
    (->sql (references :t (not-deferrable-initially-deferred))) =>
    "REFERENCES t NOT DEFERRABLE INITIALLY DEFERRED"
    (->sql (references :t (not-deferrable (initially :immediate)))) =>
    "REFERENCES t NOT DEFERRABLE INITIALLY IMMEDIATE"
    (->sql (references :t (not-deferrable-initially-immediate))) =>
    "REFERENCES t NOT DEFERRABLE INITIALLY IMMEDIATE"))

(fact "You can specify a FOREIGN KEY table constraint"
  (->sql (foreign-key (columns (column :a) (column :b))
                      (references :t [:x :y]))) =>
  "FOREIGN KEY (a, b) REFERENCES t (x, y)"
  (->sql (foreign-key (columns :a :b) (references :t [:x :y]))) =>
  "FOREIGN KEY (a, b) REFERENCES t (x, y)"
  (->sql (foreign-key [:a :b] (references :t [:x :y]))) =>
  "FOREIGN KEY (a, b) REFERENCES t (x, y)")
