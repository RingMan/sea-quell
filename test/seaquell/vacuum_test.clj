(ns seaquell.vacuum-test
  "Tests the SQLite VACUUM command"
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [me.raynes.fs :as fs]
            [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.engine :refer [db-conn executes?]]
            [seaquell.sqlite :refer [db-spec]]))

(fact "Seaquell supports the SQLite VACUUM statement"
  (vacuum$) => "VACUUM;"
  (vacuum$ :main) => "VACUUM main;" ;means same as above
  (vacuum$ :myschema) => "VACUUM myschema;" ;for an attached schema
  (vacuum$ :myschema (into "file")) => "VACUUM myschema INTO 'file';")

(fact "You can vacuum the default schema into a file"
  (vacuum$ :into "file") => "VACUUM INTO 'file';"
  (vacuum$ (into "file")) => "VACUUM INTO 'file';"
  (vacuum$ nil (into "file")) => "VACUUM INTO 'file';"
  (vacuum$ :main (into "file")) => "VACUUM main INTO 'file';"
  (vacuum-into$ "file") => "VACUUM INTO 'file';"
  (vacuum-into$ "file" (schema :main)) => "VACUUM main INTO 'file';")

(fact "You can vacuum an attached schema into a file"
  (vacuum$ :dbschema :into "file") => "VACUUM dbschema INTO 'file';"
  (vacuum$ :dbschema (into "file")) => "VACUUM dbschema INTO 'file';"
  (vacuum-into$ "file" (schema :dbschema)) => "VACUUM dbschema INTO 'file';")

(let [q1 (vacuum)
      q2 (vacuum :main)
      c (db-conn (db-spec))]
  (fact "VACUUM the default schema (main)"
        (sql$ q1) => "VACUUM;"
        (vacuum$ q1 (schema :main)) => "VACUUM main;"
        (sql$ q2) => "VACUUM main;"
        (sql! q1 (db c)) => executes?
        (sql! q2 (db c)) => executes?))

(let [f "a-file"
      s :a_schema
      q (vacuum s (into f))
      c (db-conn (db-spec))]
  (fact "vacuum and vacuum-into are idempotent"
        (vacuum q) => q
        (vacuum-into q) => q)
  (fact "VACUUM an attached schema to a file"
        (attach! ":memory:" (as s) (db c)) => executes?
        (sql! q (db c)) => executes?
        (fs/exists? f) => truthy
        (fs/delete f) => truthy))

