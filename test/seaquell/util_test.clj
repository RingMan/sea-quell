(ns seaquell.util-test
  "Tests various utility functions."
  (:refer-clojure
    :exclude [alter distinct drop group-by into set update partition-by when])
  (:require [midje.sweet :refer :all :exclude [after before]]
            [seaquell.core :refer :all]
            [seaquell.util :refer :all]))

(fact
  (hexify 0xa) => "0a"
  (hexify 0x10) => "10"
  (hexify 0xff) => "ff")

(fact (as-hex-string (byte-array [0xbe 0xef])) => "beef")

(fact
  (constraint? :not-a-map) => false
  (constraint? (check [> :x 5])) => truthy
  (constraint? (collate :nocase)) => truthy
  (constraint? (default 42)) => truthy
  (constraint? (foreign-key :fk (references :t [:c]))) => truthy
  (constraint? (generated (as [* 2 :x]))) => truthy
  (constraint? (not-null)) => truthy
  (constraint? (primary-key)) => truthy
  (constraint? (references :t)) => truthy
  (constraint? (unique)) => truthy
  (constraint? (constraint (not-null))) => truthy)

(fact
  (table-constraint? (check [> :x 5])) => truthy
  (table-constraint? (collate :nocase)) => falsey
  (table-constraint? (default 42)) => falsey
  (table-constraint? (foreign-key :fk (references :t [:c]))) => truthy
  (table-constraint? (generated (as [* 2 :x]))) => falsey
  (table-constraint? (not-null)) => falsey
  (table-constraint? (primary-key)) => falsey
  (table-constraint? (primary-key [:x])) => truthy
  (table-constraint? (references :t)) => falsey
  (table-constraint? (unique)) => falsey
  (table-constraint? (unique [:x])) => truthy
  (table-constraint? (constraint (unique))) => falsey
  (table-constraint? (constraint (unique [:x]))) => truthy)

(fact
  (fields? (fields 42)) => truthy)

(fact
  (let [q1 (value 1), q2 (value 2)]
    (set-op? :union-all (union-all q1 q2)) => truthy
    (set-op? :intersect (union-all q1 q2)) => falsey
    (set-op? :intersect :not-a-map) => falsey))
