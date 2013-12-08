(defproject ehsm "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-paymill "0.1.4-SNAPSHOT"]
                 [org.clojure/tools.trace "0.7.6"]
                 [org.clojure/tools.reader "0.7.7"]
                 [cheshire "5.2.0"]
                 [http-kit "2.1.5"]
                 [compojure "1.1.5"]
                 [clj-time "0.5.1"]
                 [ring/ring-json "0.2.0"]
                 [ring-basic-authentication "1.0.2"]
                 [ring.middleware.logger "0.4.0"]
                 [com.cemerick/url "0.1.0"]
                 [enlive "1.1.4"]
                 [clj-fo "0.0.1"]
                 [com.draines/postal "1.11.0"]]
  :jvm-opts ["-Djavax.net.ssl.trustStore=resources/cacerts" "-Xmx512m" "-Xms128m"]
  :main ehsm.core)
