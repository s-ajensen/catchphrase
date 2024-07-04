(ns catchphrase.teamc
  (:require [c3kit.bucket.api :as db]))

(defn create-team [game color]
  {:kind   :team
   :color  color
   :game   (:id game game)
   :points 0})

(defn create-team! [game color]
  (db/tx (create-team game color)))