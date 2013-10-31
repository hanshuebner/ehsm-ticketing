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
            [ring.util.response :as ring-response]
            [clj-time.core :as time-core]
            [clj-time.format :as time-format]
            [clj-paymill.net :as paymill-net]
            [clj-paymill.client :as paymill-client]
            [cheshire.core :as json]
            [ehsm.invoice :as invoice]
            [postal.core :as postal]))

(defonce default-port 7676)
(defonce paymill-private-key "f0a966a7f4d01204c4712def21a9f73d")

(defonce smtp-host "localhost")
(defonce email-from "EHSM 2014 Tickets <tickets@ehsm.eu>")
(defonce admin-email-address "tickets@ehsm.eu")

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

(defn send-invoice [order invoice-pdf]
  (postal/send-message ^{:host smtp-host}
                       {:from email-from
                        :to (:emailAddress order)
                        :subject "Invoice for your EHSM ticket"
                        :body [{:type "text/plain"
                                :content "Hi,

Thank you for registering for the Exceptionally Hard & Soft Meeting 2014!

Please find your invoice attached.  You will hear from us when we have worked out
more details of the conference.

See you at DESY in June!
-The EHSM Team"}
                               {:type :inline
                                :content invoice-pdf
                                :content-type "application/pdf"}]}))

(defn send-admin-notice [order json-pathname invoice-pdf-pathname]
  (postal/send-message ^{:host smtp-host}
                       {:from email-from
                        :to admin-email-address
                        :subject "EHSM ticket sold"
                        :body [{:type "text/plain"
                                :content "Hi,

A ticket or EHSM has been sold!  Please see the attachments for details.

-The EHSM Team"}
                               {:type :inline
                                :content invoice-pdf-pathname
                                :content-type "application/pdf"}
                               {:type :inline
                                :content json-pathname
                                :content-type "application/json"}]}))

(defn pay [req]
  (let [{:keys [paymillToken order]} (:body req)
        {:keys [donation type]} order]
    (if (and (= type "regularEarly")
             (not (early-available?)))
      {:status 423 :body "early registration is closed"}
      (let [amount (* (+ (:price (tickets type)) donation) 100)
            description (str "EHSM " (:description (tickets type)) " Ticket"
                             (when (and donation (pos? donation))
                               (str " + " donation " EUR donation")))
            result (make-paymill-transaction {:token paymillToken :amount amount :currency "EUR" :description description})]
        (if (= (:response_code result) 20000)
          (let [[json-pathname invoice-pdf-pathname] (invoice/make-invoice order (tickets type) result)]
            (send-invoice order invoice-pdf-pathname)
            (send-admin-notice order json-pathname invoice-pdf-pathname)
            {:status 200 :body "ok"})
          (do
            (println "payment failed" result)
            {:status 423 :body result}))))))

(defn not-found [req]
  {:status 404
   :body "Not found."})

(defn client-side-route [req]
  (ring-response/resource-response "index.html" {:root "public"}))

(defroutes all-routes
  (POST "/paymill-callback" [] paymill-callback)
  (POST "/pay" [] pay)
  ;; Enumerating all the AngularJS routes here is kind of cheesy, but
  ;; I'm too tired to find a more beautiful way right now.
  (GET "/" [] client-side-route)
  (GET "/buy" [] client-side-route)
  (GET "/processing" [] client-side-route)
  (GET "/done" [] client-side-route)
  (GET "/error" [] client-side-route)
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
