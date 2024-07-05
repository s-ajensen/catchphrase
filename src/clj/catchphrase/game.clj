(ns catchphrase.game
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.time :as time]
            [catchphrase.gamec :as gamec]
            [catchphrase.occupantc :as occupantc]
            [catchphrase.room :as room]
            [catchphrase.roomc :as roomc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.apic :as apic]
            [catchphrase.teamc :as teamc]))

(def lock (Object.))
(defmacro with-lock [& body]
  `(locking lock
     ~@body))

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

(defn sleep! [ms] (Thread/sleep ms))

(defn -run-round! [{:keys [round-length] :as game} room]
  (sleep! round-length)
  (with-lock
    (let [game (db/entity (:id game))
          room (db/entity (:id room))
          ;teams (teamc/by-game game)
          ;inactive-team (ccc/ffilter #(not= active-team (:id %)) teams)
          payload [(gamec/stop-round game)
                   #_(update inactive-team :points inc)]]
      (db/tx* payload)
      (room/push-to-room! room payload :game/update))))

(defn run-round! [game room]
  (future (-run-round! game room)))

(defn ws-start-game [{:keys [connection-id] :as _request}]
  (with-lock
    (let [occupant (occupantc/by-conn-id connection-id)
          room (roomc/by-occupant occupant)
          game (gamec/by-room room)]
      (or (maybe-not-host room occupant)
          (let [game (gamec/start-round! game)]
            (room/push-to-room! room [game] :game/update)
            (run-round! game room)
            (apic/ok [game]))))))

(defn maybe-not-active-occupant [game occupant]
  (when (not= (:active-occupant game) (:id occupant))
    (apic/fail nil "You can only advance the game if it is your turn!")))

(defn ws-advance-game [{:keys [connection-id] :as _request}]
  (with-lock
    (let [occupant (occupantc/by-conn-id connection-id)
          room (roomc/by-occupant occupant)
          game (gamec/by-room room)]
      (or (maybe-not-active-occupant game occupant)
          (let [game (gamec/advance-game! game)]
            (room/push-to-room! room [game] :game/update)
            (apic/ok))))))

(defn ws-steal-game [{:keys [connection-id] :as _request}]
  (with-lock
    (let [occupant (occupantc/by-conn-id connection-id)
          room (roomc/by-occupant occupant)
          game (gamec/by-room room)
          team (:team occupant)]
      (or (maybe-not-active-occupant game occupant)
          (let [other-team (ccc/ffilter #(not= team (:id %)) (db/find-by :team :game (:id game)))
                new-team (db/tx (update-in other-team [:points] inc))]
            (room/push-to-room! room [new-team] :game/update)
            (apic/ok))))))

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