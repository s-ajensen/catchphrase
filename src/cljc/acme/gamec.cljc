(ns acme.gamec
  (:require [c3kit.bucket.api :as db]))

(defn create-game []
  {:kind    :game
   :counter 0})

(defn create-game! []
  (db/tx (create-game)))

(defn by-room [room]
  (let [game-room (db/ffind-by :game-room :room (:id room room))]
    (db/entity (:game game-room))))