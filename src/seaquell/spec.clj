(ns seaquell.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::sql-stmt keyword?)

; predefined SQLite collating sequences for strings
(s/def ::collate #{:binary :nocase :rtrim})

(s/def ::order #{:asc :desc})

(s/def ::nulls #{:first :last})

(s/def ::order-term
  (s/keys :req-un [::expr] :opt-un [::collate ::order ::nulls]))

(s/def ::order-by (s/coll-of ::order-term))

(defmulti sql-stmt-type :sql-stmt)

(defmethod sql-stmt-type :select [_]
    (s/keys :req-un [::sql-stmt (or ::fields ::from)]
            :opt-un [::group-by ::having ::window ::order-by ::limit ::offset ::with]))

(defmethod sql-stmt-type :insert [_]
    (s/keys :req-un [::sql-stmt]
            :opt-un []))

(defmethod sql-stmt-type :update [_]
    (s/keys :req-un [::sql-stmt]
            :opt-un []))

(defmethod sql-stmt-type :delete [_]
    (s/keys :req-un [::sql-stmt]
            :opt-un []))

(s/def ::statement (s/multi-spec sql-stmt-type :sql-stmt))
