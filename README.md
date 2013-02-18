sea-quell
=========

Tame the stormy seas of SQL with sea-quell, another DSL for generating SQL statements

Mainly, this is a personal experiment.  It's also a wish list for what I'd love to see:

* A front-end syntax for creating SQL statements as values
  * Resembles SQL itself
  * Lets you define objects and the relationships between them
* A set of well-defined data structures for each kind of statement
* A back-end transformation to convert SQL values to SQL strings suitable to pass on to JDBC
* Support for vendor-specific transformations
* Clear separation of the front-end and back-end where the data itself is the only connection between the two

## Getting Started
First, clone `diesel` and make it available to `sequel`.

```bash
$ git clone https://github.com/RingMan/diesel.git
$ cd diesel
$ lein install
```
Now clone `sequel`, grab its dependencies, and open up a repl.

```bash
$ cd ..
$ git clone https://github.com/RingMan/sequel.git
$ cd sequel
$ lein deps
$ lein repl
```

To play with query definitions and see them as SQL strings

```
user=> (use 'sequel.core)
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
