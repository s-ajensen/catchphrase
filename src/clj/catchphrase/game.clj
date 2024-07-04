(ns catchphrase.game
  (:require [c3kit.apron.time :as time]
            [catchphrase.gamec :as gamec]
            [catchphrase.occupantc :as occupantc]
            [catchphrase.room :as room]
            [catchphrase.roomc :as roomc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.apic :as apic]
            [catchphrase.teamc :as teamc]))

(defn maybe-occupant-not-found [occupant]
  (when-not occupant (apic/fail {} "Occupant not found")))

(defn maybe-room-not-found [room]
  (when-not room
    (apic/fail {} "Room not found")))

(defn maybe-game-not-found [game]
  (when-not game
    (apic/fail {} "Game not found")))

(defn ws-fetch-game [{:keys [connection-id] :as _request}]
  (let [occupant (occupantc/by-conn-id connection-id)
        room (roomc/by-occupant occupant)
        game (gamec/by-room room)
        teams (teamc/by-game game)]
    (or (maybe-occupant-not-found occupant)
        (maybe-room-not-found room)
        (apic/ok (cons game teams)))))

(defn maybe-not-host [room occupant]
  (when-not (= (:id occupant) (first (:occupants room)))
    (apic/fail nil "Only the host can start the game!")))

(def base-len (time/seconds 40))

(defn start-round! [game]
  (db/tx (assoc game :round-start (time/now)
                     :round-length (+ base-len (time/seconds (rand-int 20))))))

(defn ws-start-game [{:keys [connection-id] :as request}]
  (let [occupant (occupantc/by-conn-id connection-id)
        room     (roomc/by-occupant occupant)
        game     (gamec/by-room room)]
    (or (maybe-not-host room occupant)
        (let [game (start-round! game)]
          (room/push-to-room! room [game] :game/update)
          (apic/ok [game])))))

(defn inc-counter! [game]
  (db/tx (update game :counter inc)))

(defn inc-n-dispatch! [room game]
  (room/push-to-room! room (inc-counter! game) :game/update)
  (apic/ok))

(defn ws-inc-counter [{:keys [connection-id] :as _request}]
  (let [occupant (occupantc/by-conn-id connection-id)
        room (roomc/by-occupant occupant)
        game (gamec/by-room room)]
    (or (maybe-occupant-not-found occupant)
        (maybe-room-not-found room)
        (maybe-game-not-found game)
        (inc-n-dispatch! room game))))