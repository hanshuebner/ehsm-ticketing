(ns ehsm.invoice
  (:use [clojure.contrib.math :only [ceil floor]])
  (:require [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [clj-fo.fo :as fo]
            [clojure.string :as string]))

(defn base-price [euro-amount]
  (/ (ceil (float (* (/ (* euro-amount 100) 11900) 10000))) 100))

(defn vat [euro-amount]
  (/ (floor (float (* (/ (* euro-amount 100) 11900) 1900))) 100))

(defn create-invoice [invoice-number recipient ticket donation]
  (let [price (:price ticket)
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
               (map fo/block (string/split recipient #"\n")))
     (fo/block {:font-weight "bold"
                :font-size "16"
                :space-after.optimum "20pt"}
               "Invoice")
     (fo/table {:border-width "0.5pt" :space-after.optimum "30pt"}
               ["4cm" "5cm"]
               [["Date:" (time-format/unparse
                          (time-format/formatters :date)
                          (time-core/now))]
                ["Invoice number:" invoice-number]])
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
               "Your payment has been received through Paymill.  We're looking forward to seeing you at the conference!"))))

#_
(fo/write-pdf!
 (create-invoice
  "EHSM01"
  "Hans Hübner
Strelitzer Straße 63
10115 Berlin"
  {:description "Supporter" :price 272}
  nil)
 "invoice-website-acme.pdf")
