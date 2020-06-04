(ns seaquell.raw
  "Experimental namespace for representing SQL using sequential data
  structures instead of maps. Would let users generate arbitrary
  SQL. Similar to hiccup for HTML."
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [seaquell.util :refer [regex?]]))

(s/def ::raw string?)

(s/def ::raw-elem (s/or :raw (s/keys :req-un [::raw]) :re regex?))

(s/def ::sql-elem
  (s/or :nil nil? :t true? :f false? :num number?  :ch char? :str string?
        :kw keyword? :sym symbol? :fn fn? :list list? :vec vector? :raw ::raw-elem))

(s/def ::sql (s/coll-of ::sql-elem))

(defprotocol RawSqlCompiler
  "Protocol for compiling sequences of raw SQL tokens"
  (sqlize [this s] "Compiles the SQL entities in `s` to a string")
  (baz [a] [a b] "baz docs"))

(def sqlite-kws
  #{:abort :action :add :after :all :alter :always :analyze :and :as :asc :attach
    :autoincrement :before :begin :between :by :cascade :case :cast :check :collate
    :column :commit :conflict :constraint :create :cross :current :current_date
    :current_time :current_timestamp :database :default :deferrable :deferred :delete
    :desc :detach :distinct :do :drop :each :else :end :escape :except :exclude :exclusive
    :exists :explain :fail :filter :first :following :for :foreign :from :full :generated
    :glob :group :groups :having :if :ignore :immediate :in :index :indexed :initially
    :inner :insert :instead :intersect :into :is :isnull :join :key :last :left :like :limit
    :match :natural :no :not :nothing :notnull :null :nulls :of :offset :on :or :order
    :others :outer :over :partition :plan :pragma :preceding :primary :query :raise :range
    :recursive :references :regexp :reindex :release :rename :replace :restrict :right
    :rollback :row :rows :savepoint :select :set :table :temp :temporary :then :ties :to
    :transaction :trigger :unbounded :union :unique :update :using :vacuum :values :view
    :virtual :when :where :window :with :without})

(def sqlite-core-fns
  #{:abs :coalesce :hex :ifnull :length :lower :ltrim :max :min :nullif :rtrim :upper})

(def sqlite-date-time-fns
  #{:date :time :datetime :julianday :strftime})

(def sqlite-aggregate-fns
  #{:avg :count :group_concat :max :min :sum :total})

(def sqlite-window-fns
  #{:row_number :rank :dense_rank :percent_rank :cume_dist :ntile
    :lag :lead :first_value :last_value :nth_value})

(def sqlite-fns
  (set/union sqlite-core-fns sqlite-date-time-fns sqlite-aggregate-fns sqlite-window-fns))

(def fn->sym
  {+ '+, - '-, * '*, / '/
   < '<, <= '<=, = '=, not= '<>, >= '>=, > '>
   not 'not, max 'max, min 'min, count 'count, mod 'mod})

(defn as-name [x]
  (if (fn? x) (fn->sym x) x))

(defn sql-kw? [f]
  (let [f (as-name f)]
    (when (or (keyword? f) (symbol? f))
      (-> f name str/lower-case keyword sqlite-kws))))

(defn sql-fn? [f]
  (let [f (as-name f)]
    (when (or (keyword? f) (symbol? f))
      (-> f name str/lower-case keyword sqlite-fns))))

(defn val? [x]
  (or (= 'clojure.core/val x) (= clojure.core/val x)))

(defn vals? [x]
  (or (= 'clojure.core/vals x) (= clojure.core/vals x)))

(defn vec? [x]
  (or (= 'clojure.core/vec x) (= clojure.core/vec x)))

(defn list-fn? [x]
  (or (= 'clojure.core/list x) (= clojure.core/list x)))

(defn map-fn? [x]
  (or (= 'clojure.core/map x) (= clojure.core/map x)))

(defn name->sql [x]
  (if (not= (name x) "-")
    (let [xs (->> (str/split (name x) #"-")
                  (map keyword)
                  (map #(if (sql-kw? %) (str/upper-case (name %)) (name %)))
                  (str/join " "))]
      xs)
    x))

(comment
  [val 1 2 3] => (\, 1 2 3)
  [vals 1 2 3] => [\, (1) (2) (3)]
  [vals [1 2] [3 4]] => [\, (\, 1 2) (\, 3 4)])

(defn tuples* [n xs]
  {:pre [(= 0 (mod (count xs) n))]}
  (->> xs (partition n) (map #(cons \, %)) (into [\,])))

(defn tuples [n & xs]
  (tuples* n xs))

(defn scalars [& xs]
  (tuples* 1 xs))

(defn pairs [& xs]
  (tuples* 2 xs))

(defn trips [& xs]
  (tuples* 3 xs))

(defn quads [& xs]
  (tuples* 4 xs))

(defn quints [& xs]
  (tuples* 5 xs))

(defn values* [n xs]
  [:values (tuples* n xs)])

(defn values [n & xs]
  (values* n xs))

(declare ->sql)

(defn array* [n xs]
  {:pre [(= 0 (mod (count xs) n))]}
  [:array
   (apply vector vec (->> xs (partition n) (map #(->> % (cons vec) vec))))])

(defn array [n & xs]
  (array* n xs))

(defn curly* [n xs]
  {:pre [(= 0 (mod (count xs) n))]}
  (apply vector map (->> xs (partition n) (map #(->> % (cons map) vec)))))

(defn curly [n & xs]
  (curly* n xs))

(defn ->sql
  "Converts a sequence of sql elements to a SQL string"
  [s [x & xs :as q]]
  (let [elems xs]
    #_(println "s x, xs, q"  s x xs q)
    (cond
      (empty? q) s
      (nil? x) (recur (conj s "NULL") xs)
      (true? x) (recur (conj s "TRUE") xs)
      (false? x) (recur (conj s "FALSE") xs)
      (number? x) (recur (conj s x) xs)
      (keyword? x) (recur (conj s (name->sql x)) xs)
      (symbol? x) (recur (conj s (name->sql x)) xs)
      (fn? x) (recur (conj s (name->sql (fn->sym x))) xs)
      (char? x) (recur (conj s x) xs)
      (string? x) (recur (conj s (str \' (str/escape x {\' "''"}) \')) xs)
      (vector? x)
      (let [[y & ys] x]
        (cond
          (char? y) (recur (conj s (str/join (str y " ") (map #(str/join " " (->sql [] [%])) ys))) xs)
          (vec? y) (recur (conj s (str \[ (str/join (str ", ") (map #(str/join " " (->sql [] [%])) ys)) \])) xs)
          (map-fn? y) (recur (conj s (str \{ (str/join (str ", ") (map #(str/join " " (->sql [] [%])) ys)) \})) xs)
          (list-fn? y) (recur s (cons (cons \, ys) xs))
          (val? y) (recur s (cons (cons \, ys) xs))
          (vals? y) (recur s (cons (->> ys (map #(if (sequential? %) (cons \, %) (list %))) (cons \,) vec) xs))
          (sql-fn? y) (recur (conj s (str (name (as-name y)) \( (str/join (str ", ") (map #(str/join " " (->sql [] [%])) ys)) \))) xs)
          :else (recur (conj s (str/join " " (->sql [] [(first x)]))) (concat (rest x) xs))))
      (sequential? x)
      (let [[y & ys] x]
        (if (char? y)
          (recur (conj s (str \( (str/join (str y " ") (map #(str/join " " (->sql [] [%])) ys)) \))) xs)
          (recur (conj s (str \( (str/join " " (->sql [] x)) \))) xs)))
      (s/valid? ::raw-elem x) (recur (conj s (if (regex? x) (str x) (:raw x))) xs)
      :else (throw (RuntimeException. (str "invalid raw sql element: `" x "`")))
      )))

(def compiler
  (reify RawSqlCompiler
    (sqlize [this s]
      (str/join " " (->sql [] s)))
    #_(baz [this])
    #_(baz [this arg])))

(defn sql [& xs] (vec xs))

(defn sql$
  ([s] (sql$ compiler s))
  ([c s] (sqlize c s)))

