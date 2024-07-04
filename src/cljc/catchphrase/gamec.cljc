(ns catchphrase.gamec
  (:require [c3kit.apron.time :as time]
            [c3kit.bucket.api :as db]
            [catchphrase.teamc :as teamc]))

(defn create-game []
  {:kind    :game
   :counter 0})

(defn create-game! []
  (let [game (db/tx (create-game))
        blu-team (teamc/create-team! game :blu)]
    (teamc/create-team! game :red)
    (db/tx game :active-team (:id blu-team))))

(defn by-room [room]
  (let [game-room (db/ffind-by :game-room :room (:id room room))]
    (db/entity (:game game-room))))

(def base-len (time/seconds 40))

(defn start-round [game]
  (assoc game :state :started
              :round-start (time/now)
              :round-length (+ base-len (time/seconds (rand-int 20)))))