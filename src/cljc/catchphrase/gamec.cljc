(ns catchphrase.gamec
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.time :as time]
            [c3kit.bucket.api :as db]
            [catchphrase.teamc :as teamc]))

(defn create-game []
  {:kind    :game
   :state   :lobby
   :counter 0})

(defn create-game! []
  (let [game (db/tx (create-game))
        blu-team (teamc/create-team! game :blu)]
    (teamc/create-team! game :red)
    (db/tx game :active-team (:id blu-team))))

(defn by-room [room]
  (let [game-room (db/ffind-by :game-room :room (:id room room))]
    (db/entity (:game game-room))))

(def base-len (time/seconds 100))

(defn start-round [game]
  (assoc game :state :started
              :round-start (time/now)
              :round-length (+ base-len (time/seconds (rand-int 20)))
              :active-occupant (first (:occupants (db/entity (:room (db/ffind-by :game-room :game (:id game game))))))))
; ^ product of bad design. occupants should really be a game thing

(defn start-round! [game]
  (db/tx (start-round game)))

(defn stop-round [game]
  (assoc game :state :round-end))

(defn stop-round! [game]
  (db/tx (stop-round game)))

(defn drop-until [pred coll]
  (drop-while (complement pred) coll))

(defn next-occupant [game]
  (let [room (db/entity (:room (db/ffind-by :game-room :game (:id game game))))
        occupants (map db/entity (:occupants room))
        [blu-team red-team] (-> (group-by :team occupants) vals vec)
        rotation (interleave (cycle blu-team) (cycle red-team))]
    (if (= 1 (count occupants))
      (first occupants)
      (db/entity (second (drop-until #(= (:active-occupant game) (:id %)) rotation))))))

(defn advance-game! [game]
  (db/tx game :active-occupant (:id (next-occupant game))))