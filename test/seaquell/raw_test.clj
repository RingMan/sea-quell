(ns seaquell.raw-test
  "Tests the raw SQL DSL provided by Seaquell"
  (:require [clojure.spec.alpha :as s]
            [midje.sweet :refer :all]
            [seaquell.core :refer [raw, sql$]]
            [seaquell.raw :as r]))

(fact "Use `raw` or a regular expression to represent literal SQL text"
  (s/valid? ::r/raw-elem #"create table t (x, y);") => truthy
  (s/valid? ::r/raw-elem (raw "create table t (x, y)")) => truthy
  (s/valid? ::r/raw-elem {:raw "create table t (x, y)"}) => truthy
  (s/valid? ::r/raw-elem :not-a-raw-token) => falsey)

(fact "Raw SQL is passed through unchanged"
  (sql$ (raw "select 'raw';")) => "select 'raw';"
  (fact "Leading strings to `sql` are treated as verbatim SQL, not string tokens"
    (sql$ "select *" "from t" "where x = 'hi';")
    => "select * from t where x = 'hi';")
  (fact "A regex is treated as a raw SQL literal"
    (sql$ #"select 'raw';") => "select 'raw';")
  (fact "But a regex won't work for abitrary SQL strings since some characters
        have special meaning in a regex."
    (read-string "(sql$ #\"[]\")") => (throws #"Unclosed character class")))

(fact "Single quotes in strings are escaped"
  (sql$ ["She said, 'Hi'"]) => "'She said, ''Hi'''")

(fact "Characters are converted to their string representation"
  (sql$ \;) => ";"
  (sql$ \() => "("
  (sql$ \tab) => "\t")

(fact "Numbers are converted to their string representation"
  (sql$ 5) => "5"
  (sql$ 1.23) => "1.23"
  (sql$ 4.56e+6) => (str 4.56e+6))

(fact "nil becomes NULL"
  (sql$ nil) => "NULL")

(fact "boolean constants are capitalized"
  (sql$ false) => "FALSE"
  (sql$ true) => "TRUE")

(fact "keywords and symbols are split on hyphens and then capitalized if a
      SQL keyword"
  (sql$ :select) => "SELECT"
  (sql$ :create-table-if-not-exists) => "CREATE TABLE IF NOT EXISTS"
  (sql$ 'insert-into) => "INSERT INTO")

(fact "Clojure functions that have a SQL counterpart can be used directly"
  (fact "This includes the basic arithmetic operators: +, -, *, /, mod"
    (sql$ +) => "+"
    (sql$ mod) => "mod")
  (fact "This includes relational operators: < <= = not= >= > and logical not"
    (sql$ >) => ">"
    (sql$ not=) => "<>"
    (sql$ not) => "NOT")
  (fact "It also includes aggregate functions min, max, and count"
    (sql$ count) => "count"))

(fact "Vectors are treated special"
  (fact "If the first element is a character, it is used as a separator."
    (sql$ [\, 1 2 3]) => "1, 2, 3")
  (fact "If the first element is the clojure.core/vec fn or symbol,
        the elements are separated by commas and wrapped in square brackets"
    (sql$ [vec 1 2]) => "[1, 2]"
    (sql$ [`vec 1 2]) => "[1, 2]"
    (sql$ ['clojure.core/vec 1 2]) => "[1, 2]"
    (sql$ ['vec 1 2]) => "vec 1 2")
  (fact "If the first element is the clojure.core/list fn or symbol,
        the elements are separated by commas and wrapped in parentheses"
    (sql$ [list 1 2]) => "(1, 2)"
    (sql$ [`list 1 2]) => "(1, 2)"
    (sql$ ['clojure.core/list 1 2]) => "(1, 2)"
    (sql$ ['list 1 2]) => "list 1 2")
  (fact "If the first element is the clojure.core/map fn or symbol,
        the elements are separated by commas and wrapped in curly braces"
    (sql$ [map 1 2]) => "{1, 2}"
    (sql$ [`map 1 2]) => "{1, 2}"
    (sql$ ['clojure.core/map 1 2]) => "{1, 2}"
    (sql$ ['map 1 2]) => "map 1 2")
  (fact "If the first element is the clojure.core/val fn or symbol,
        the elements are separated by commas and wrapped in parentheses"
    (sql$ [val 1 2]) => "(1, 2)"
    (sql$ [`val 1 2]) => "(1, 2)")
  (fact "If the first element is the clojure.core/vals fn or symbol,
        the elements are separated by commas and wrapped in parentheses"
    (sql$ [vals 1 [2 3] [4 5] 6]) => "(1), (2, 3), (4, 5), (6)"
    (sql$ [`vals 1 [2 3] [4 5] 6]) => "(1), (2, 3), (4, 5), (6)")
  (fact "If the first element is recognized as a SQL function, the vector is
        treated like a function call, where the first element is the function
        name, and the rest are function arguments."
    (sql$ '[coalesce nil 5 "hi"]) => "coalesce(NULL, 5, 'hi')")
  (fact "If the first element is not recognized as special, the vector is
        simply used for grouping. The elements are spliced into the SQL stream"
    (sql$ [[1 [2 [3 [4 5]]]]]) => "1 2 3 4 5")
  )

(fact "Lists are also special. They are parenthesized. If the first element
      is a character, it is used as a separator."
  (sql$ '(1 2 3)) => "(1 2 3)"
  (sql$ '(\, 1 2 3)) => "(1, 2, 3)")

(fact "An unrecognized SQL element throws"
  (sql$ [{:my :invalid-raw-sql}])
  => (throws #"invalid raw sql element: `\{:my :invalid-raw-sql}`"))

(fact "It's easy to create tuples to use in a VALUES clause"
  (fact "You can create 1-tuples with `scalars`"
    (sql$ (r/scalars 1 2 3)) => "(1), (2), (3)")
  (fact "You can create 2-tuples with `pairs`"
    (sql$ (r/pairs 1 2 3 4)) => "(1, 2), (3, 4)")
  (fact "You can create 3-tuples with `trips`"
    (sql$ (r/trips 1 2 3 4 5 6)) => "(1, 2, 3), (4, 5, 6)")
  (fact "You can create 4-tuples with `quads`"
    (sql$ (r/quads 1 2 3 4 5 6 7 8)) => "(1, 2, 3, 4), (5, 6, 7, 8)")
  (fact "You can create 5-tuples with `quints`"
    (sql$ (r/quints 1 2 3 4 5 6 7 8 9 10))
    => "(1, 2, 3, 4, 5), (6, 7, 8, 9, 10)")
  (fact "You can create n-tuples with `tuples`, with arity as the first arg"
    (sql$ (r/tuples 3 1 2 3 4 5 6)) => "(1, 2, 3), (4, 5, 6)")
  (fact "You can use the values function to create an entire VALUES clause"
    (sql$ (r/values 3 1 2 3 4 5 6)) => "VALUES (1, 2, 3), (4, 5, 6)"))

(fact "These functions may be useful for two-dimensional PostgreSQL arrays"
  (sql$ (r/array 2 1 2 3 4)) => "array [[1, 2], [3, 4]]"
  (sql$ (r/curly 2 1 2 3 4)) => "{{1, 2}, {3, 4}}")

;; The remaining functions are not part of the public API

(facts "about `as-name` helper fn"
  (r/as-name >) => '>
  (r/as-name not=) => '<>
  (r/as-name map) => nil
  (r/as-name -not-a-fn-) => -not-a-fn-)

(facts "about sql-kw?"
  (r/sql-kw? :SeLecT) => truthy
  (r/sql-kw? 'SeLecT) => truthy
  (r/sql-kw? not) => truthy)

(fact "the `seaquell.raw/sql` function just collects its args in a vector"
  (r/sql 1 :a 'b "c") => [1 :a 'b "c"])
