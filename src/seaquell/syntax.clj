(ns seaquell.syntax
  (:require [clojure.spec.alpha :as s]))

(s/def ::id keyword?)

(s/def ::expr any?)

(s/def ::pos-num (s/and number? pos?))

(def <expr> :expr)
(def <ordering-item> :ordering-item)
(def <pos-num> :pos-num)
(def <bound> #{{:preceding <pos-num>}
               {:following <pos-num>}
               :current-row
               :unbounded-preceding
               :unbounded-following})
(def <id> :id)

(def <windef>
  {:base-win :kw-or-nil
   :partition-by [<expr>]
   :order-by [<ordering-item>]
   :bounds [<bound>]
   :exclude #{:no-others :group :current-row :ties}})

(def <id-or-windef (or <id> <windef>))

(def field
  {:field <id>
   :as <id>
   :filter-where <expr>
   :over <id-or-windef})

(defn windef? [w] (and (map? w) (contains? w :base-win)))

(comment
  (over :w1) => {:over :w1}
  (over :w1 (partition-by :a)) => {:base-win :w1, :partition-by [:a]}
  (over (partition-by :a)) => {:base-win nil, :partition-by [:a]}
  
  (win :w1 :as (windef ...)) => {:wins {:win :w1 :as <windef>}}
  (win :w1 (as (windef ...))) => {:wins {:win :w1 :as <windef>}}
  (win :w1 :as [windef args ...]) => {:wins {:win :w1 :as <windef>}}
  (win :w1 (as [windef args ...])) => {:wins {:win :w1 :as <windef>}}
  (win :w1 [windef args ...]) => {:wins {:win :w1 :as <windef>}}
  (win <windef> ...) => (mk-map* <windef> ...)
  )

