(ns catchphrase.styles.layout.document
  (:refer-clojure :exclude [rem])
  (:require [catchphrase.styles.core :refer :all]))

(def screen
(list

  ["@font-face" {
                 :font-family (str "'TF2 BUILD'")}
   {:src        (str "url('/fonts/tf2build.ttf')")
    :font-weight "normal"
    :font-style  "normal"
    }]

[:body :html
 {:margin 0
  :padding 0
  :font-family font-family
  :color "#333"}]

[:h1
 {:font-size "24px"
  :text-align "center"
  :margin-bottom "20px"}]

["input[type=\"text\"]"
 {:width "250px"
  :padding "10px"
  :margin "5px"
  :font-size "16px"
  :color "#333"
  :border "1px solid"
  :border-radius "4px"
  :font-family "inherit"
  :text-align "center"}]

[:button
 {:padding "10px"
  :font-size "16px"
  :border "none"
  :font-family "inherit"
  :border-radius "5px"
  :cursor "pointer"
  :transition "background-color 0.3s"
  :margin "5px"}]

[:ul
 {:list-style-type "none"
  :padding "0"}]

))
