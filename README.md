seaquell [![Build Status](https://travis-ci.org/RingMan/sea-quell.png)](https://travis-ci.org/RingMan/sea-quell)
=========

*Tame the stormy seas of SQL with seaquell, another Clojure DSL for generating SQL statements*

Mainly, this is a personal experiment.  It's also a wish list for what I'd love to see:

* A front-end syntax for creating SQL statements as values
  * Resembles SQL itself
  * Lets you define objects and the relationships between them
* A set of well-defined data structures for each kind of statement
* A back-end transformation to convert SQL values to SQL strings suitable to pass on to JDBC
* Support for vendor-specific transformations
* Clear separation of the front-end and back-end where the data itself is the only connection between the two

## Getting Started
To create a new leiningen project that downloads `seaquell` from [clojars](https://clojars.org/seaquell):
```bash
$ lein new my-app
$ cd my-app
```
Edit `project.clj` so your project looks something like this...
```clojure
(defproject my-app "0.1.0-SNAPSHOT"
  ;; ...
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [seaquell "0.1.0"]])
```

To work from source, clone `seaquell`, grab its dependencies, and open up a repl.

```bash
$ git clone https://github.com/RingMan/sea-quell.git
$ cd sea-quell
$ lein deps
$ lein repl
```

To play with query definitions and see them as SQL strings

```
user=> (use 'seaquell.core)
user=> (def q1 (select :* (from :users)))
user=> (def q2 (select [:id :passwd] (from :users)))
user=> (to-sql q1)
```

Finally, connect to a database and execute your queries

```
user=> (use 'korma.db)
user=> (defdb mydb (sqlite3 {:db "/path/to/sqlite.db"}))
user=> (do-sql q2)
```
## Documentation and Examples
Check out the [wiki](https://github.com/RingMan/sea-quell/wiki) to learn more about using `seaquell`
and how it's designed. For some full-featured examples, take a peek at these [answers](src/seaquell/zoo/select_within_select.clj)
to the [SQL Zoo](http://sqlzoo.net) *Select within Select* tutorial.

## Testing
To check your installation or if you choose to contribute, you may want to execute the units tests.
Sea-quell uses the excellent [midje](https://github.com/marick/Midje) library for this purpose.
Just type `lein midje` from the command prompt to run the tests.  They should all pass.

Looking at the [tests](test/seaquell/core_test.clj) is a great way to learn what sea-quell can and can't do.

## A Note About MS Access
For those of you who work with Microsoft Access, you'll probably want to switch to the `paren-joins` branch.
Unlike `master`, the `paren-joins` branch is subject to rebasing (you've been warned), but it has a few
differences that make it work well with Access.

The biggest difference is that multiple joins are parenthesized left-to-right. Next, `join` is the same
as `inner-join`. I think this is true for most dialects, but Access seems to require one of INNER, RIGHT,
or LEFT before the JOIN keyword. Join conditions that use logical connectives also get parentheses.
Again, queries that break without them work when they're added. Finally, I changed the delimiters to square
brackets (reads nicer).

**IMPORTANT**: When you're doing joins targeting an Access database, you'll need to qualify all column names with
the table they came from, even if the column name is unambiguous. This may be good practice anyway, but
Access insists on it.
