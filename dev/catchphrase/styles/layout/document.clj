(ns catchphrase.styles.layout.document
  (:refer-clojure :exclude [rem])
  (:require [catchphrase.styles.core :refer :all]))

(def screen
(list

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
  :text-align "center"}]

[:button
 {:width "120px"
  :padding "10px"
  :font-size "16px"
  :border "none"
  :border-radius "5px"
  :cursor "pointer"
  :transition "background-color 0.3s"
  :margin "5px"}]

[:ul
 {:list-style-type "none"
  :padding "0"}]

))
