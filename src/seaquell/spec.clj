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

;; Primary statements

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

;; Explain statements

(defmethod sql-stmt-type :explain [_]
    (s/keys :req-un [::sql-stmt ::statement]))

(defmethod sql-stmt-type :explain-query-plan [_]
    (s/keys :req-un [::sql-stmt ::statement]))

;; Attach/Detach

(defmethod sql-stmt-type :attach [_]
    (s/keys :req-un [::sql-stmt ::database ::as] :opt-un [::modifier]))

(defmethod sql-stmt-type :detach [_]
    (s/keys :req-un [::sql-stmt ::as] :opt-un [::modifier]))

(s/def ::statement (s/multi-spec sql-stmt-type :sql-stmt))
