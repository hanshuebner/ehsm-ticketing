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
  (println "PAYMILL-CALLBACK" (:body req))
  {:status 200
   :body "ok"})

(defn pay [req]
  (println "PAY token" (:paymillToken (:params req)))
  {:status 200
   :body "ok"})

(defn not-found [req]
  {:status 404
   :body "Not found."})

(defroutes all-routes
  (GET "/paymill-callback" [] paymill-callback)
  (POST "/pay" [] pay)
  (route/not-found not-found))

(defn app []
  (-> all-routes
      (ring-logger/wrap-with-plaintext-logger)
      (ring-resource/wrap-resource "public")
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
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
  (start-server))
