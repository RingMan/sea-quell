(defproject seaquell "0.3.2-SNAPSHOT"
  :description "Tame the stormy seas of SQL with sea-quell, another DSL for generating SQL statements"
  :url "https://github.com/RingMan/sea-quell"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [diesel "0.1.3-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.1"]
                                  [midje "1.9.9"]
                                  [org.xerial/sqlite-jdbc "3.30.1"]
                                  [mysql/mysql-connector-java "5.1.23"]
                                  [org.hsqldb/hsqldb "2.2.9"]]
                   :source-paths ["dev"]
                   :plugins [[lein-midje "3.2.2"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :aliases {"test-all" ["with-profile" "dev,1.7:dev,1.8:dev,1.9" "midje"]
            "check-all" ["with-profile" "1.7:1.8:1.9" "check"]})

