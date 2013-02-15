(ns sequel.core-test
  (:use midje.sweet
        sequel.core))

(fact (sequel) => :awesome)
