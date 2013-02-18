(defproject seaquell "0.1.0-SNAPSHOT"
  :description "Tame the stormy seas of SQL with sea-quell, another DSL for generating SQL statements"
  :url "https://github.com/RingMan/sea-quell"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [diesel "0.1.0-SNAPSHOT"]
                 [korma "0.3.0-RC4"]]
  :profiles {:dev {:dependencies [[midje "1.5-beta1"]
                                  [org.xerial/sqlite-jdbc "3.7.2"]]
                   :plugins [[lein-midje "3.0-beta1"]]}})
