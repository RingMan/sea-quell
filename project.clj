(defproject seaquell "0.3.2-SNAPSHOT"
  :description "Tame the stormy seas of SQL with sea-quell, another DSL for generating SQL statements"
  :url "https://github.com/RingMan/sea-quell"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [diesel "0.1.3-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.1"]
                                  [integrant "0.8.0"]
                                  [midje "1.9.9"]
                                  [org.xerial/sqlite-jdbc "3.31.1"]
                                  [mysql/mysql-connector-java "5.1.23"]
                                  [org.hsqldb/hsqldb "2.2.9"]
                                  [clj-commons/fs "1.5.2"]]
                   :plugins [[lein-midje "3.2.2"]]}
             :repl {:dependencies [[expound "0.8.4"]
                                   [integrant/repl "0.3.1"]]
                    :source-paths ["dev"]}
             :test {:dependencies [[clj-commons/fs "1.5.2"]
                                   [midje "1.9.9"]
                                   [org.xerial/sqlite-jdbc "3.31.1"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :aliases {
            ;"test-all" ["with-profile" "dev,1.7:dev,1.8:dev,1.9" "midje"]
            "test-all" ["with-profile" "test,1.9:test" "midje"]
            "check-all" ["with-profile" "1.9:test" "check"]})

