(ns seaquell.sql-ex-pieces-providers
  "Uses an in-memory Sqlite database to do the SQL exercises at
  https://en.wikibooks.org/wiki/SQL_Exercises/Pieces_and_providers"
  (:refer-clojure :exclude [update partition-by])
  (:require [clojure.java.jdbc :as jdb])
  (:require [diesel.core :refer [mk-map*]])
  (:use midje.sweet
        seaquell.core
        seaquell.engine))

(defn properties [& body]
  (mk-map* {} body))

(def sq3 {:classname "org.sqlite.JDBC"
          :subprotocol "sqlite"
          :subname ":memory:"})

(defn create-tbls [con]
  (do-sql
    "CREATE TABLE Pieces (
       Code INTEGER PRIMARY KEY NOT NULL,
       Name TEXT NOT NULL
    );"
    (db con))
  (do-sql
    "CREATE TABLE Providers (
      Code TEXT PRIMARY KEY NOT NULL,
      Name TEXT NOT NULL
    );"
    (db con))
  (do-sql
    "CREATE TABLE Provides (
      Piece INTEGER  
        CONSTRAINT fk_Pieces_Code REFERENCES Pieces(Code),
      Provider TEXT
        CONSTRAINT fk_Providers_Code REFERENCES Providers(Code),
      Price INTEGER NOT NULL,
      PRIMARY KEY(Piece, Provider)
    );"
    (db con)))

(defn insert-data [con]
  (let [providers (insert :Providers (columns :Code :Name) (db con))
        pieces (insert :Pieces (columns :Code :Name) (db con))
        provides (insert :Provides (columns :Piece :Provider :Price) (db con))]
    (insert! providers (value "HAL" "Clarke Enterprises"))
    (insert! providers (value "RBT" "Susan Calvin Corp."))
    (insert! providers (value "TNBC" "Skellington Supplies"))

    (insert! pieces (values [1 "Sprocket"] [2 "Screw"] [3 "Nut"] [4 "Bolt"]))

    (insert! provides
             (values [1 "HAL" 10] [1 "RBT" 15] [2 "HAL" 20]
                     [2 "RBT" 15] [2 "TNBC" 14] [3 "RBT" 50]
                     [3 "TNBC" 45] [4 "HAL" 5] [4 "RBT" 7]))))

(defn mk-db []
  (let [c (->> sq3 jdb/get-connection
               (jdb/add-connection sq3))]
    (create-tbls c)
    (insert-data c)
    c))

(defn kw [x] (keyword x))

(let [c (mk-db)]
  (fact
    "Select the name of all the pieces"
    (select! :name (from :pieces) (db c))
    => [{:name "Sprocket"} {:name "Screw"}
        {:name "Nut"}  {:name "Bolt"}])
  
  (fact
    "Select all the providers' data"
    (select! :* (from :providers) (db c))
    => [{:code "HAL" :name "Clarke Enterprises"}
        {:code "RBT" :name "Susan Calvin Corp."}
        {:code "TNBC" :name "Skellington Supplies"}])

  (fact
    "Obtain the average price of each piece
    (show only the piece code and the average price)."
    (select! [:piece [:avg :price]] (from :provides) (group :piece) (db c))
    => [{(kw "avg(price)") 12.5 :piece 1}
        {(kw "avg(price)") 16.333333333333332 :piece 2}
        {(kw "avg(price)") 47.5 :piece 3}
        {(kw "avg(price)") 6.0 :piece 4}])
  
  (fact
    "Obtain the names of all providers who supply piece 1."
    ;; using INNER JOIN
    (select!
      :providers.name
      (db c)
      (from :providers
            (inner-join :provides (on {:providers.code :provides.provider
                                       :provides.piece 1}))))
    => [{:name "Clarke Enterprises"} {:name "Susan Calvin Corp."}]
    ;; using IN predicate
    (select!
      :name
      (db c)
      (from :providers)
      (where {:code [:in (select :provider
                                 (from :provides)
                                 (where [= :piece 1]))]}))
    => [{:name "Clarke Enterprises"} {:name "Susan Calvin Corp."}])

  (fact
    "Select the name of pieces provided by provider with code 'HAL'."
    ;; using INNER JOIN
    (select!
      :pieces.name
      (db c)
      (from :pieces
            (inner-join :provides (on {:pieces.code :provides.piece
                                       :provides.provider "HAL"}))))
    => [{:name "Sprocket"} {:name "Screw"} {:name "Bolt"}]
    ;; using IN predicate
    (select!
      :name
      (db c)
      (from :pieces)
      (where [:in :code (select :piece
                                (from :provides)
                                (where {:provider "HAL"}))]))
    => [{:name "Sprocket"} {:name "Screw"} {:name "Bolt"}]
    ;; using EXISTS predicate
    (select!
      :name
      (db c)
      (from :pieces)
      (where [:exists (select-from
                        :provides
                        (where {:provider "HAL"
                                :piece :pieces.code}))]))
    => [{:name "Sprocket"} {:name "Screw"} {:name "Bolt"}])

  (fact
    "For each piece, find the most expensive offering of that piece and
    include the piece name, provider name, and price  (note that there could
    be two providers who supply the same piece at the most expensive price)."
    (select!
      [:pieces.name (as :piece) :providers.name :as :provider :price]
      (db c)
      (from
        :pieces
        (inner-join :provides (on {:pieces.code :piece}))
        (inner-join :providers (on {:providers.code :provider})))
      (where {:price (select [[max :price]]
                             (from :provides)
                             (where {:piece :pieces.code}))}))
    => [{:piece "Sprocket" :price 15 :provider "Susan Calvin Corp."}
        {:piece "Screw" :price 20 :provider "Clarke Enterprises"}
        {:piece "Nut" :price 50 :provider "Susan Calvin Corp."}
        {:piece "Bolt" :price 7 :provider "Susan Calvin Corp."}]))

(let [c (mk-db)]
  (fact
    "Add an entry to the database to indicate that  'Skellington Supplies'
    (code  'TNBC') will provide sprockets  (code  '1') for 7 cents each."
    (insert! :provides (value 1 "TNBC" 7) (db c))
    => [1]
    ;; Confirm new record, notice use of row-fn, result-set-fn
    (select! :provider (from :provides) (where {:piece 1 :price 7})
             (db c) (row-fn :provider) (result-set-fn first))
    => "TNBC"))

(let [c (mk-db)]
  "Increase all prices by one cent."
  (fact
    (update! :provides (set-cols {:price [+ :price 1]}) (db c))
    => [9]
    ;; Confirm raise in price
    (select-from! :provides (db c))
    => [{:piece 1 :price 11 :provider "HAL"}
        {:piece 1 :price 16 :provider "RBT"}
        {:piece 2 :price 21 :provider "HAL"}
        {:piece 2 :price 16 :provider "RBT"}
        {:piece 2 :price 15 :provider "TNBC"}
        {:piece 3 :price 51 :provider "RBT"}
        {:piece 3 :price 46 :provider "TNBC"}
        {:piece 4 :price 6 :provider "HAL"}
        {:piece 4 :price 8 :provider "RBT"}]))

(let [c (mk-db)
      w-db (properties (where {:provider "RBT" :piece 4}) (db c))]
  (fact
    "Update the database to reflect that 'Susan Calvin Corp.' (code 'RBT')
    will not supply bolts (code 4)."
    (select! :* (from :provides) w-db)
    => [{:piece 4, :price 7, :provider  "RBT"}]
    (delete! :provides w-db) => [1]
    (select! :* (from :provides) w-db) => []))

(let [c (mk-db)
      w-db (properties (where {:provider "RBT"}) (db c))]
  (fact
    "Update the database to reflect that 'Susan Calvin Corp.' (code 'RBT')
    will not supply any pieces (the provider should still remain in the
    database)."
    (delete! :provides w-db) => [4]
    (select-from! :provides w-db) => []))

