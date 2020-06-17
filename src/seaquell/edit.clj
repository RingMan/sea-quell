(ns seaquell.edit
  (:require [diesel.edit :refer [as-vec conjv-in replace-in replace-nth-in rm-in rm-nth-in]]
            [seaquell.core :refer [fields from]]))

;;; Helpers for composing queries
(defn and-expr? [x] (and (sequential? x) (#{:and 'and "and"} (first x))))
(defn or-expr? [x] (and (sequential? x) (#{:or 'or "or"} (first x))))
(defn xor-expr? [x] (and (sequential? x) (#{:xor 'xor "xor"} (first x))))

(defn and-where
  "Returns a function to AND `expr` with existing WHERE clause"
  [expr]
  (fn [q]
    (let [w (:where q)]
      (assoc q :where (if (and-expr? w) (conj (vec w) expr) [:and w expr])))))

(defn or-where
  "Returns a function to OR `expr` with existing WHERE clause"
  [expr]
  (fn [q]
    (let [w (:where q)]
      (assoc q :where (if (or-expr? w) (conj (vec w) expr) [:or w expr])))))

(defn xor-where
  "Returns a function to XOR `expr` with existing WHERE clause"
  [expr]
  (fn [q]
    (let [w (:where q)]
      (assoc q :where (if (xor-expr? w) (conj (vec w) expr) [:xor w expr])))))

(defn not-where
  "Returns a function to negate existing WHERE clause"
  []
  (fn [q]
    (let [w (:where q)]
      (assoc q :where [not w]))))

(defn and-having
  "Returns a function to AND `expr` with existing HAVING clause"
  [expr]
  (fn [q]
    (let [w (:having q)]
      (assoc q :having (if (and-expr? w) (conj (vec w) expr) [:and w expr])))))

(defn or-having
  "Returns a function to OR `expr` with existing HAVING clause"
  [expr]
  (fn [q]
    (let [w (:having q)]
      (assoc q :having (if (or-expr? w) (conj (vec w) expr) [:or w expr])))))

(defn xor-having
  "Returns a function to XOR `expr` with existing HAVING clause"
  [expr]
  (fn [q]
    (let [w (:having q)]
      (assoc q :having (if (xor-expr? w) (conj (vec w) expr) [:xor w expr])))))

(defn not-having
  "Returns a function to negate existing HAVING clause"
  []
  (fn [q]
    (let [w (:having q)]
      (assoc q :having [not w]))))

(defn cons-fields
  "Returns a function to prepend one or more fields to a query"
  [& xs]
  (fn [q]
    (let [flds (concat (:fields (apply fields xs)) (as-vec (:fields q)))]
      (assoc q :fields flds))))

(def cons-field cons-fields)

(defn conj-fields
  "Returns a function to append one or more fields to a query"
  [& xs]
  (fn [q]
    (let [flds (concat (as-vec (:fields q)) (:fields (apply fields xs)))]
      (assoc q :fields flds))))

(def conj-field conj-fields)
(def also-fields conj-fields)
(def also-field conj-fields)

(defn cons-from [& xs]
  "Returns a function to prepend `xs` to an existing FROM clause
  using the same syntax as the `from` function."
  (fn [q]
    (let [f (concat (:from (apply from xs)) (as-vec (:from q)))]
      (assoc q :from f))))

(defn conj-from
  "Returns a function to append `xs` to an existing FROM clause
  using the same syntax as the `from` function."
  [& xs]
  (fn [q]
    (let [f (concat (as-vec (:from q)) (:from (apply from xs)))]
      (assoc q :from f))))

(def also-from conj-from)

(defn cons-order-by [& xs]
  (fn [q] (assoc q :order-by (concat xs (as-vec (:order-by q))))))

(defn conj-order-by [& xs]
  (fn [q] (apply conjv-in q [:order-by] xs)))

(def also-order-by conj-order-by)

(defn cons-group-by [& xs]
  (fn [q] (assoc q :group-by (concat xs (as-vec (:group-by q))))))

(defn conj-group-by [& xs]
  (fn [q] (apply conjv-in q [:group-by] xs)))

(def also-group-by conj-group-by)

(defn ins-fields [n & xs]
  (fn [q]
    (let [xs (:fields (apply fields xs))
          [ws ys] (split-at n (as-vec (:fields q)))]
      (assoc q :fields (concat ws xs ys)))))

(def ins-field ins-fields)

(defn ins-from [n & xs]
  (fn [q]
    (let [xs (:from (apply from xs))
          [ws ys] (split-at n (as-vec (:from q)))]
      (assoc q :from (concat ws xs ys)))))

(defn ins-group-by [n & xs]
  (fn [q]
    (let [[ws ys] (split-at n (as-vec (:group-by q)))]
      (assoc q :group-by (concat ws xs ys)))))

(defn ins-order-by [n & xs]
  (fn [q]
    (let [[ws ys] (split-at n (as-vec (:order-by q)))]
      (assoc q :order-by (concat ws xs ys)))))

(defmacro mk-rm-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "rm-" (name x))) ~'[& xs]
             (fn [~'q] (apply rm-in ~'q [~(keyword x)] ~'xs))))))

(mk-rm-fns fields from group-by order-by)
(def rm-field rm-fields)

(defmacro mk-rm-nth-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "rm-nth-" (name x))) ~'[& xs]
             (fn [~'q] (apply rm-nth-in ~'q [~(keyword x)] ~'xs))))))

(mk-rm-nth-fns fields from group-by order-by)
(def rm-nth-field rm-nth-fields)

(defmacro mk-replace-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "replace-" (name x))) ~'[smap]
             (fn [~'q] (replace-in ~'q [~(keyword x)] ~'smap))))))

(mk-replace-fns fields from group-by order-by)
(def replace-field replace-fields)

(defmacro mk-replace-nth-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "replace-nth-" (name x))) ~'[smap]
             (fn [~'q] (replace-nth-in ~'q [~(keyword x)] ~'smap))))))

(mk-replace-nth-fns fields from group-by order-by)
(def replace-nth-field replace-nth-fields)

(defmacro mk-edit-nth-fns [& xs]
  (cons
    'do
    (for [x xs]
      `(defn ~(symbol (str "edit-nth-" (name x))) ~'[n f & args]
        (fn [~'q]
          (-> ~'q
              (update-in [~(keyword x)] as-vec)
              ((partial apply update-in) [~(keyword x) ~'n] ~'f ~'args)))))))

(mk-edit-nth-fns fields from group-by order-by)
(def edit-nth-field edit-nth-fields)

(defmacro mk-edit-fns [& xs]
  (cons 'do
        (for [x xs]
          `(defn ~(symbol (str "edit-" (name x))) ~'[f & args]
             (fn [~'q] (apply update-in ~'q [~(keyword x)] ~'f ~'args))))))

(mk-edit-fns fields from where group-by having limit offset)

(defn rm-clauses [& ks]
  (fn [q] (apply dissoc q ks)))

(def rm-parts rm-clauses)

