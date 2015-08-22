(defproject seaquell "0.3.0"
  :description "Tame the stormy seas of SQL with sea-quell, another DSL for generating SQL statements"
  :url "https://github.com/RingMan/sea-quell"
  :scm {:name "git"
        :url "https://github.com/RingMan/sea-quell"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [diesel "0.1.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [midje "1.7.0"]
                                  [org.xerial/sqlite-jdbc "3.8.10.1"]
                                  [mysql/mysql-connector-java "5.1.23"]
                                  [org.hsqldb/hsqldb "2.2.9"]]
                   :source-paths ["dev"]
                   :plugins [[lein-midje "3.1.3"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :aliases {"test-all" ["with-profile" "dev,1.4:dev,1.5:dev,1.6:dev,1.7" "midje"]
            "check-all" ["with-profile" "1.4:1.5:1.6:1.7" "check"]})

