(defproject fhir "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/core.async "1.5.640"]
                 [clj-time "0.15.2"]
                 [datascript "1.3.2"]

                 [aysylu/loom "1.0.2"]
                 [selmer "1.12.44"]]
  ;;[com.github.artronics/resource-locator "0.1.0-SNAPSHOT"]]

  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.0"]]}}

  :dev {:resource-paths ["test/resources"]}
  :repl-options {:init-ns com.github.artronics.fhir.core})
