(ns catchphrase.game
  (:require [catchphrase.gamec :as gamec]
            [catchphrase.occupantc :as occupantc]
            [catchphrase.room :as room]
            [catchphrase.roomc :as roomc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.apic :as apic]))

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
        game (gamec/by-room room)]
    (or (maybe-occupant-not-found occupant)
        (maybe-room-not-found room)
        (apic/ok [game]))))

(defn ws-start-game [request]
  )

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