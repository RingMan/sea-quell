(ns seaquell.util)

(defn raw? [x] (and (map? x) (= [:raw] (keys x))))

(defn alias? [x] (and (map? x) (= (keys x) [:as])))

(defn fields? [x] (and (map? x) (= (keys x) [:fields])))

(defn sql-stmt?
  ([x] (:sql-stmt x))
  ([stmt-type x] (= (:sql-stmt x) stmt-type)))

(def update? (partial sql-stmt? :update))

(def delete? (partial sql-stmt? :delete))

(def insert? (partial sql-stmt? :insert))

(def select? (partial sql-stmt? :select))

(def compound-select? (partial sql-stmt? :compound-select))

(defn set-op? [set-op stmt]
  (and (compound-select? stmt) (= (:set-op stmt) set-op)))

(def union? (partial set-op? :union))
(def union-all? (partial set-op? :union-all))
(def intersect? (partial set-op? :intersect))
(def intersect-all? (partial set-op? :intersect-all))
(def except? (partial set-op? :except))
(def except-all? (partial set-op? :except-all))

