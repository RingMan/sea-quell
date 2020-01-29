(ns seaquell.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::sql-stmt keyword?)

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
