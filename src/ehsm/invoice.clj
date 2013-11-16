(ns ehsm.invoice
  (:use [clojure.contrib.math :only [ceil floor]])
  (:require [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [clj-fo.fo :as fo]
            [clojure.string :as string]
            [cheshire.core :as json]))

(def invoices-directory "invoices")

(defn base-price [euro-amount]
  (/ (ceil (float (* (/ (* euro-amount 100) 11900) 10000))) 100))

(defn vat [euro-amount]
  (/ (floor (float (* (/ (* euro-amount 100) 11900) 1900))) 100))

(defn create-invoice-fo [invoice-number order payment-info]
  (let [{:keys [invoiceInfo ticket donation]} order
        price (:price ticket)
        base-price (base-price price)
        vat (vat price)
        donation (if (and donation (pos? donation))
                   (float donation)
                   nil)
        total (+ price (or donation 0.0))]
    (fo/document
     {:header-blocks (map #(fo/block {:text-align "end"} %)
                          ["Exceptionally Hard and Soft Meeting e.V."
                           "c/o Marco Machicao y Priemer"
                           "Wiesbadener Straße 28"
                           "D-14197 Berlin"
                           "Germany"])}
     (fo/block {:font-size "35pt"
                :space-before.optimum "0pt"
                :space-after.optimum "15pt"}
               "EHSM e.V.")
     (fo/block {:space-before.optimum "100pt"
                :space-after.optimum "20pt"}
               (map fo/block (string/split (or invoiceInfo "") #"\n")))
     (fo/block {:font-weight "bold"
                :font-size "16"
                :space-after.optimum "20pt"}
               "Invoice")
     (fo/table {:border-width "0.5pt" :space-after.optimum "30pt"}
               ["4cm" "5cm"]
               [["Date:" (time-format/unparse
                          (time-format/formatters :date)
                          (time-core/now))]
                ["Invoice number:" (str invoice-number)]])
     (fo/table {:border-width "0.5pt"}
               ["14cm" "3cm"]
               (concat
                [[(str (:description ticket) " Ticket")
                  (fo/block {:text-align "right"}
                            (format "€ %.2f" base-price))]
                 ["19% VAT"
                  (fo/block {:text-align "right"}
                            (format "€ %.2f" vat))]]
                (when donation
                  [["Donation"
                    (fo/block {:text-align "right"}
                              (format "€ %.2f" donation))]])
                [[{:font-weight "bold"}
                  "Total:"
                  (fo/block {:text-align "right"}
                            (format "€ %.2f" total))]]))
     (fo/block {:space-before.optimum "35pt"}
               (map fo/block (string/split (or payment-info "") #"\n")))
     (fo/block "We're looking forward to seeing you at the conference!"))))

(defn create-invoice-pdf [pathname & args]
  (fo/write-pdf! (apply create-invoice-fo args) pathname))

(def invoice-directory (java.io.File. "invoices"))

(defn find-last-invoice-number []
  (reduce (fn [previous-max file]
            (max previous-max
                 (if-let [invoice-number-string (first (rest (re-matches #"invoice-0*(\d+).json" (.getName file))))]
                   (read-string invoice-number-string)
                   previous-max)))
          1
          (file-seq invoice-directory)))

(def last-invoice-number (atom (find-last-invoice-number)))

(defn next-invoice-number []
  (swap! last-invoice-number inc))

(defn make-invoice [order payment-result payment-info]
  (io!
   (let [invoice-number (next-invoice-number)
         file-basename (format "%s/invoice-%04d" invoices-directory invoice-number)
         pdf-pathname (str file-basename ".pdf")
         json-pathname (str file-basename ".json")]
     (create-invoice-pdf pdf-pathname
                         invoice-number
                         order
                         payment-info)
     (spit json-pathname (json/generate-string {:invoiceNumber invoice-number
                                                :order order
                                                :paymentResult payment-result}
                                               {:pretty true}))
     [json-pathname pdf-pathname])))

#_
(create-invoice-pdf
  "invoices/invoice-1.pdf"
  1
  "Hans Hübner
Strelitzer Straße 63
10115 Berlin"
  {:description "Supporter" :price 272}
  30)
