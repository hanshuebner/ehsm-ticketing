(ns ehsm.invoice
  (:require [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [clj-fo.fo :as fo]
            [clojure.string :as string]))

(defn create-invoice [invoice-number nodes recipient]
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
   (let [subtotal (apply +
                         (map (fn [{:keys [type full-amount discount]}]
                                (if (= type :+)
                                  full-amount
                                  (* -1 discount full-amount)))
                              nodes))
         vat (* 0.21 subtotal)
         total (+ subtotal vat)]
     (fo/table {:border-width "0.5pt"}
               ["14cm" "3cm"]
               (concat
                (map (fn [{:keys [type description full-amount discount]}]
                       [(str description ":")
                        (fo/block {:text-align "right"}
                                  (format "€ %.2f"
                                          (if (= type :+)
                                            full-amount
                                            (* -1 discount full-amount))))])
                     nodes)
                [["Subtotal:"
                  (fo/block {:text-align "right"}
                            (format "€ %.2f" subtotal))]
                 ["Value Added Tax (21%):"
                  (fo/block {:text-align "right"}
                            (format "€ %.2f" vat))]
                 [{:font-weight "bold"}
                  "Total:"
                  (fo/block {:text-align "right"}
                            (format "€ %.2f" total))]])))
   (fo/block {:space-before.optimum "35pt"}
             (str "You are kindly requested to pay within 7 days. "
                  "Please wire the amount due to Rabobank account "
                  "number 3285.04.165."))))

(fo/write-pdf!
 (create-invoice
  "EHSM01"
  [{:type :+
    :description "Regular conference ticket"
    :full-amount 70.0}]
  "Hans Hübner
Strelitzer Straße 63
10115 Berlin")
 "invoice-website-acme.pdf")
