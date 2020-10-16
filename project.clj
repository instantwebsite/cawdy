(defproject cawdy "0.3.0"
  :description "Library for interacting with the Caddy Admin API"
  :url "https://github.com/victorb/cawdy"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cheshire "5.10.0"]
                 [clj-http "3.10.3"]
                 [lispyclouds/clj-docker-client "1.0.1"]]
  :repl-options {:init-ns cawdy.core})
