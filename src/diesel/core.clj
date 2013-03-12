(ns diesel.core
  (:require [clojure.string :as str]))

(def pct-units #{:pct :percent :dec})

(defn mk-prop [k v]
  {k v})

(defn mk-unit-prop [k v u]
  {k {:val v :units u}})

(defn mk-pct-prop [k v u]
  (assert (pct-units u))
  {k (str (double (if (= :dec u) (* 100 v) v)) " percent")})

(defn mk-loc-prop [k lat lon]
  {k {:lat lat :lon lon}})

(defmacro mk-prop-makers [mk-prop-func & syms]
  (cons 'do
        (for [sym syms]
          `(def ~sym (partial ~mk-prop-func ~(keyword sym))))))

(defmacro def-props [& syms]
  `(mk-prop-makers mk-prop ~@syms))

(defmacro def-pct-props [& syms]
  `(mk-prop-makers mk-pct-prop ~@syms))

(defmacro def-unit-props [& syms]
  `(mk-prop-makers mk-unit-prop ~@syms))

(defmacro def-loc-props [& syms]
  `(mk-prop-makers mk-loc-prop ~@syms))

(def N :N)
(def S :S)
(def E :E)
(def W :W)

(def % :percent)

(defn unit [v u] {:val v :units u})

(defn lat [& args]
  {:lat
   (apply str (interpose " " (map #(if (keyword %) (name %) %) args)))})

(defn lon [& args]
  {:lon
   (apply str (interpose " " (map #(if (keyword %) (name %) %) args)))})

(defn mk-map* [m args]
  (let [[x & xs] args]
    (cond
      (nil? args) m
      (nil? x) (recur m xs)
      (map? x) (recur (merge m x) xs)
      (coll? x) (recur m (concat x xs))
      (fn? x) (recur (x m) xs)
      (keyword? x) (recur (assoc m x (first xs)) (rest xs))
      :else (throw (RuntimeException.
                     (str "Illegal arg to mk-map*: " x))))))

(defn properties [& body]
  (mk-map* {} body))

;Next two funcs define some generic syntax useful for creating lists of
;named items
;Example:
;(defn widgets [& body]
;  {:widgets (mk-map* {} body)})
;
;(widgets (item :toaster :cost 3 :qty 2) (item :phone :cost 5 :qty 5))
;
;=> {:widgets {:toaster {:cost 3 :qty 2}
;              :phone   {:cost 5 :qty 5}}}
(defn item [id & body]
  {id (mk-map* {} body)})

(defn entry [id & body]
  {id (mk-map* {} body)})

(defn alter-in
  "Merges args into map
  If an arg is a keyword, the next arg is taken to be its value
  Map args are merged in
  Function args are invoked with the current map passed in"
  [m & args]
  (mk-map* m args))

(defmacro def-entity-macros
  "Creates an entity-generating macro for each symbol in syms.
  For example (def-entity-macros scenario) creates a macro like

  (defmacro defscenario [id & body]
    `(def ~id (scenario ~(name id) ~@body)))"
  [& syms]
  (cons 'do
        (for [sym syms]
          `(defmacro ~(symbol (str "def" (name sym))) [~'id & ~'body]
             `(def ~~'id ('~~sym ~~'(keyword (name id)) ~@~'body))))))

