(ns acme.styles.components.menus
  (:refer-clojure :exclude [rem])
  (:require [acme.styles.core :refer :all]))

(def screen
(list

[:.homepage-container
 {:display "flex"
  :flex-direction "column"
  :align-items "center"
  :justify-content "center"
  :height "100vh"}]

[:.nickname-input :.room-actions
 {:margin-bottom "15px"}]

[:.room-actions
 {:display "flex"
  :justify-content "center"
  :gap "10px"}]

[:.main-container {
                   :display "grid"
                   :grid-template-columns "1fr 1fr 1fr"
                   }]

[:.left-container {
                   :float "right"
                   :text-align "right"
                   }]

[:.center {
           :display "flex"
           :justify-content "center"
           }]


[:.game-container
 {:padding "20px"
  :border "2px solid"
  :border-radius "8px"
  :box-shadow "0 2px 5px rgba(0,0,0,0.2)"
  :max-width "600px"
  :margin "20px"}]

[:.user-list
 {:border "2px solid #d2b48c"
  :border-radius "8px"
  :box-shadow "0 2px 5px rgba(0,0,0,0.2)"
  :width "200px"
  :margin "20px"
  :padding "10px"
  }]

[:.categories
 {:display               "grid"
  :grid-template-columns "auto 1fr"
  :row-gap "10px"
  :column-gap "10px"}]

[:.categories-data
 {:font-size "18px"
  :padding "10px"
  :border-radius "5px"}]
))
