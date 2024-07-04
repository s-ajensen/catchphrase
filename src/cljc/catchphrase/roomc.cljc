(ns catchphrase.roomc
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]
            [catchphrase.gamec :as gamec]
            [catchphrase.occupantc :as occupantc]))

(defn ->room [code]
  {:kind      :room
   :code      code
   :occupants []})

(defn create-room! [code]
  (let [code code
        room (->room code)]
    (db/tx room)))

(defn by-code [code]
  (db/ffind-by :room :code code))
(defn by-occupant [occupant]
  (db/ffind :room :where {:occupants [(occupantc/or-id occupant)]}))

(defn add-occupant [{:keys [occupants] :as room} occupant]
  (let [id (occupantc/or-id occupant)
        occupants (conj occupants id)]
    (assoc room :occupants occupants)))

(defn next-team [game]
  (let [blu (db/ffind-by :team :game (:id game) :color :blu)
        red (db/ffind-by :team :game (:id game) :color :red)
        blu-ct (db/count-by :occupant :team (:id blu))
        red-ct (db/count-by :occupant :team (:id red))]
    (if (> blu-ct red-ct)
      red
      blu)))

; dependency inversion is crying right now
(defn assign-team [occupant]
  (let [room (by-occupant occupant)
        game (gamec/by-room room)
        team (next-team game)]
    (assoc occupant :team (:id team))))

(defn join-room! [room occupant]
  (let [room (db/tx (add-occupant room occupant))]
    (db/tx (assign-team occupant))
    room))

(defn remove-occupant [{:keys [occupants] :as room} occupant]
  (let [id (occupantc/or-id occupant)
        occupants (remove #{id} occupants)]
    (assoc room :occupants occupants)))

(defn remove-occupant! [room occupant]
  (db/tx (remove-occupant room occupant)))

(defn room-empty? [room]
  (empty? (:occupants room)))