(ns catchphrase.game-roomc
  (:require [catchphrase.gamec :as gamec]
            [catchphrase.roomc :as roomc]
            [c3kit.bucket.api :as db]))

(defn create-game-room!
  ([code]
   (let [game (gamec/create-game!)
         room (roomc/create-room! code)]
     (create-game-room! game room)))
  ([game room]
   (db/tx {:kind :game-room
           :game (:id game game)
           :room (:id room room)})))

(defn by-room [room]
  (db/ffind-by :game-room :room (:id room room)))