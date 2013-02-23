(ns seaquell.to-sql
  (:require [clojure.string :as string]))

;;; SQL Generation

(defn expr-to-sql [x]
  (cond
    (number? x) (str x)
    (map? x) (string/join
               " AND "
               (map (fn [[k v]] (str (name k) " = " (name v))) x))
    :else (name x)))

(def modifier-map
  {:all "ALL "
   :distinct "DISTINCT "
   nil "" })

(def order-map
  {:asc " ASC"
   :desc " DESC"
   nil "" })

(defn order-item? [x] (:expr x))

(defn order-item [x]
  (if (order-item? x)
    (let [{:keys [collate expr order]} x
          collate (when collate (str " COLLATE " (name collate)))
          order (order-map order)]
      (map #(str (expr-to-sql %) collate order) expr))
    (expr-to-sql x)))

(defn order-by-clause [xs]
  (when xs
    (let [items (string/join ", " (flatten (map order-item xs)))]
      (str "ORDER BY " items))))

(defmulti to-sql :sql-stmt)

(defmethod to-sql :default [x]
  (throw (RuntimeException. (str "to-sql not implemented for "
                                 (:sql-stmt x) " statement"))))

(defn select-clause ^{:testable true}
  [modifier fields]
  (let [modifier (modifier-map modifier)
        fields (string/join ", " (map #(name (:field %)) fields))]
    (str "SELECT " modifier fields)))

(defn join-but-nils [sep xs]
  (string/join sep (keep identity xs)))
(def join-by-space (partial join-but-nils " "))
(def join-by-comma (partial join-but-nils ", "))

(defn delimit [l r x]
  (str l x r))
(def in-parens (partial delimit "(" ")"))
(def in-quotes (partial delimit "\"" "\""))

(declare join-op-to-sql)

(defn join-src-to-sql [src]
  (cond
    (keyword? src) (name src)
    (string? src) src
    (map? src) (let [{:keys [select table as]} src]
                 (cond
                   table (str (name table) (when as (str " AS " (name as))))
                   select (str (in-parens (to-sql select false))
                               (when as (str " AS " (name as))))))
    (coll? src) (in-parens (join-by-space (map join-op-to-sql src)))))

(defn field-to-sql [x]
  (name x))
(defn to-sql-keywords [x]
  (if (keyword? x)
    (let [parts (string/split (name x) #"-")]
      (->> parts (map string/upper-case) (join-by-space)))
    x))

(defn join-op-to-sql [{:keys [source op on using] :as join}]
  (if source
    (let [on (when on (str "ON " (expr-to-sql on)))
          using (when using
                  (str "USING " (-> (map field-to-sql using)
                                    (join-by-comma) (in-parens))))]
      (join-by-space [(to-sql-keywords op) (join-src-to-sql source) (or on using)]))
    (name join)))

(defn from-clause [from]
  (when from
    (let [from (if (coll? from) from [from])]
      (str "FROM " (string/join " " (map join-op-to-sql from))))))

(defn where-clause [w] (when w (str "WHERE " (expr-to-sql w))))
(defn group-clause [group]
  (when group
    (str "GROUP BY " (string/join ", " (map expr-to-sql group)))))
(defn having-clause [having]
  (when having (str "HAVING " (expr-to-sql having))))
(defn limit-clause [l] (when l (str "LIMIT " (expr-to-sql l))))
(defn offset-clause [o] (when o (str "OFFSET " (expr-to-sql o))))

(defn select-clauses [xs semi]
  (str (string/join " " (keep identity xs)) semi))

(defmethod to-sql :select
  ([stmt]
   (to-sql stmt true))
  ([{:keys [fields modifier from where group having
            order-by limit offset] :as stmt}
    semi?]
   (let [select (select-clause modifier fields)
         from (from-clause from)
         where (where-clause where)
         group (group-clause group)
         having (having-clause having)
         order-by (order-by-clause order-by)
         limit (limit-clause limit)
         offset (offset-clause offset)
         semi (when semi? ";")
         qry (str "SELECT " modifier fields from where group having
                  order-by limit offset semi)]
     (select-clauses [select from where group having
                      order-by limit offset] semi))))

