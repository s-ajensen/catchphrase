(ns catchphrase.roomc
  (:require [c3kit.bucket.api :as db]
            [catchphrase.occupantc :as occupantc]))

(defn ->room [code]
  {:kind      :room
   :code      code
   :occupants []})

(defn create-room! [code]
  (let [code code
        room (->room code)]
    (db/tx room)))

(defn add-occupant [{:keys [occupants] :as room} occupant]
  (let [id      (occupantc/or-id occupant)
        occupants (conj occupants id)]
    (assoc room :occupants occupants)))

(defn add-occupant! [room occupant]
  (db/tx (add-occupant room occupant)))

(defn remove-occupant [{:keys [occupants] :as room} occupant]
  (let [id (occupantc/or-id occupant)
        occupants (remove #{id} occupants)]
    (assoc room :occupants occupants)))

(defn remove-occupant! [room occupant]
  (db/tx (remove-occupant room occupant)))

(defn room-empty? [room]
  (empty? (:occupants room)))

(defn by-code [code]
  (db/ffind-by :room :code code))
(defn by-occupant [occupant]
  (db/ffind :room :where {:occupants [(occupantc/or-id occupant)]}))