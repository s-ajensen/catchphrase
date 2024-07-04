(ns catchphrase.game-roomc
  (:require [catchphrase.gamec :as gamec]
            [catchphrase.roomc :as roomc]
            [c3kit.bucket.api :as db]))

(defn create-game-room! [code]
  (let [game (gamec/create-game!)
        room (roomc/create-room! code)]
    (db/tx {:kind :game-room
            :game (:id game)
            :room (:id room)})))