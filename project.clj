(defproject seaquell "0.1.1"
  :description "Tame the stormy seas of SQL with sea-quell, another DSL for generating SQL statements"
  :url "https://github.com/RingMan/sea-quell"
  :scm {:name "git"
        :url "https://github.com/RingMan/sea-quell"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [diesel "0.1.1"]
                 [korma "0.3.0-RC4"]]
  :profiles {:dev {:dependencies [[midje "1.5-beta1"]
                                  [org.xerial/sqlite-jdbc "3.7.2"]
                                  [mysql/mysql-connector-java "5.1.23"]
                                  [org.hsqldb/hsqldb "2.2.9"]]
                   :plugins [[lein-midje "3.0-beta1"]]}})
