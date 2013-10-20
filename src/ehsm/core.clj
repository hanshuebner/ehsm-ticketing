(ns ehsm.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [org.httpkit.server :as server]
            [ring.middleware.json :as ring-json]
            [ring.middleware.logger :as ring-logger]
            [ring.middleware.resource :as ring-resource]
            [ring.middleware.session :as ring-session]
            [clj-paymill.net :refer :all]
            [clj-paymill.client :refer :all]))

(defonce default-port 7676)

(defn paymill-callback [req]
  (println req)
  {:status 200
   :body "ok"})

(defroutes all-routes
  (GET "/paymill-callback" [] paymill-callback))

(defn app []
  (-> all-routes
      (ring-logger/wrap-with-plaintext-logger)
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-resource/wrap-resource "public")
      (ring-session/wrap-session)
      (handler/site)))

(defonce stop-server-fn (atom nil))

(defn stop-server []
  (when @stop-server-fn
    (@stop-server-fn))
  (reset! stop-server-fn nil))

(defn start-server [& {:keys [user-directory port]}]
  (stop-server)
  (when user-directory
    (reset! user-directory user-directory))
  (reset! stop-server-fn (server/run-server (app)
                                            {:port (or port default-port)})))

(defn -main [& args]
  ;; work around dangerous default behaviour in Clojure
  (start-server))
