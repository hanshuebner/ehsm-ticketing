(ns ehsm.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [org.httpkit.server :as server]
            [ring.middleware.json :as ring-json]
            [ring.middleware.logger :as ring-logger]
            [ring.middleware.resource :as ring-resource]
            [ring.middleware.session :as ring-session]
            [ring.middleware.file-info :as ring-file-info]
            [clj-time.core :as time-core]
            [clj-time.format :as time-format]
            [clj-paymill.net :as paymill-net]
            [clj-paymill.client :as paymill-client]
            [ehsm.invoice :as invoice]))

(defonce default-port 7676)
(defonce paymill-private-key "f0a966a7f4d01204c4712def21a9f73d")

(defonce tickets {"student" {:price 45 :description "Student / Unemployed"}
                  "regularEarly" {:price 70 :description "Regular (Early registration)"}
                  "regularLate" {:price 95 :description "Regular (Late registration)"}
                  "supporter" {:price 272 :description "Supporter"}
                  "goldSupporter" {:price 1337 :description "Gold Supporter"}})

(defonce early-ticket-deadline (time-core/date-time 2014 2 2))

(defn early-available? []
  (not (time-core/after? (time-core/now) early-ticket-deadline)))

(defn paymill-callback [req]
  (println "PAYMILL-CALLBACK" (:body req))
  {:status 200
   :body "ok"})

(defn make-paymill-transaction [request]
  (paymill-net/paymill-request paymill-private-key :post
                               "transactions"
                               {"amount" (:amount request)
                                "currency" (or (:currency request) "EUR")
                                "token" (:token request)
                                "description" (:description request)}))

(def error-strings {10001 "General undefined response."
                    10002 "Still waiting on something."

                    20000 "General success response."

                    40000 "General problem with data."
                    40001 "General problem with payment data."
                    40100 "Problem with credit card data."
                    40101 "Problem with cvv."
                    40102 "Card expired or not yet valid."
                    40103 "Limit exceeded."
                    40104 "Card invalid."
                    40105 "Expiry date not valid."
                    40106 "Credit card brand required."
                    40200 "Problem with bank account data."
                    40201 "Bank account data combination mismatch."
                    40202 "User authentication failed."
                    40300 "Problem with 3d secure data."
                    40301 "Currency / amount mismatch"
                    40400 "Problem with input data."
                    40401 "Amount too low or zero."
                    40402 "Usage field too long."
                    40403 "Currency not allowed."

                    50000 "General problem with backend."
                    50001 "Country blacklisted."
                    50100 "Technical error with credit card."
                    50101 "Error limit exceeded."
                    50102 "Card declined by authorization system."
                    50103 "Manipulation or stolen card."
                    50104 "Card restricted."
                    50105 "Invalid card configuration data."
                    50200 "Technical error with bank account."
                    50201 "Card blacklisted."
                    50300 "Technical error with 3D secure."
                    50400 "Decline because of risk issues."
                    50500 "General timeout."
                    50501 "Timeout on side of the acquirer."
                    50502 "Risk management transaction timeout."
                    50600 "Duplicate transaction."})

(defn pay [req]
  (let [{:keys [paymillToken ticket]} (:body req)
        {:keys [donation type participantName participantProject emailAddress invoiceInfo]} ticket]
    (if (and (= type "regularEarly")
             (not (early-available?)))
      {:status 423 :body "early registration is closed"}
      (let [amount (* (+ (:price (tickets type)) donation) 100)
            description (str "EHSM " (:description (tickets type)) " Ticket"
                             (when (and donation (pos? donation))
                               (str " + " donation " EUR donation")))
            result (make-paymill-transaction {:token paymillToken :amount amount :currency "EUR" :description description})]
        (if (= (:response_code result) 20000)
          (do
            #_
            (send-invoice emailAddress invoiceInfo (:description (tickets type)) (:amount (tickets type)) donation)
            {:status 200 :body "ok"})
          (do
            (println "payment failed" result)
            {:status 423 :body "payment failed"}))))))

(defn not-found [req]
  {:status 404
   :body "Not found."})

(defroutes all-routes
  (POST "/paymill-callback" [] paymill-callback)
  (POST "/pay" [] pay)
  (route/not-found not-found))

(defn app []
  (-> all-routes
      (ring-logger/wrap-with-plaintext-logger)
      (ring-resource/wrap-resource "public")
      (ring-file-info/wrap-file-info)
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
