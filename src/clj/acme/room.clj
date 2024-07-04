(ns acme.room
  (:require [c3kit.bucket.api :as db]
            [c3kit.wire.apic :as apic]
            [acme.dispatch :as dispatch]
            [acme.occupantc :as occupantc]
            [acme.roomc :as roomc]
            [acme.gamec :as gamec]
            [acme.game-roomc :as game-roomc]))

(def lock (Object.))
(defmacro with-lock [& body]
  `(locking lock
     ~@body))

(def code-chars
  (->> (concat (range 48 58) (range 65 91))
       (map char)
       (remove #{\O \0 \1 \I \G \g})))

(defn new-code []
  (->> (repeatedly #(rand-nth code-chars))
       (take 6)
       (apply str)))

(defn unused-code []
  (->> (repeatedly new-code)
       (remove #(db/ffind-by :room :code %))
       first))

(defn ws-create-room [_request]
  (with-lock
    (let [code (unused-code)]
      (game-roomc/create-game-room! code)
      (apic/ok [code]))))

(defn maybe-missing-room [{:keys [room-code] :as _params}]
  (when-not room-code (apic/fail nil "Missing room!")))
(defn maybe-nonexistent-room [room]
  (when-not room (apic/fail nil "Room does not exist!")))
(defn maybe-missing-nickname [{:keys [nickname] :as params}]
  (when-not nickname (apic/fail nil "Missing nickname!")))

(defn ws-fetch-room [{:keys [params] :as _request}]
  (let [room (db/ffind-by :room :code (:room-code params))
        occupants (map db/entity (:occupants room))]
    (or (maybe-missing-room params)
        (maybe-nonexistent-room room)
        (apic/ok (cons room occupants)))))

(defn push-to-room!
  ([room payload]
   (push-to-room! room payload :room/update))
  ([room payload method]
   (let [occupants (map db/entity (:occupants room))]
     (dispatch/push-to-occupants! occupants method payload))))

(defn push-room! [room]
  (push-to-room! room [room]))

(defn- create-and-join! [room nickname connection-id]
  (let [occupant (occupantc/create-occupant! nickname connection-id)
        room (roomc/add-occupant! room occupant)
        occupants (map db/entity (:occupants room))]
    (push-to-room! room [room occupant])
    (apic/ok (cons room occupants))))

(defn- assign-to-room! [{:keys [room-code nickname]} connection-id]
  (let [room (db/ffind-by :room :code room-code)]
    (or (maybe-nonexistent-room room)
        (create-and-join! room nickname connection-id))))

(defn ws-join-room [{:keys [params connection-id] :as _request}]
  (with-lock
    (or (maybe-missing-room params)
        (maybe-missing-nickname params)
        (assign-to-room! params connection-id))))

(defn maybe-delete-room [room]
  (when (roomc/room-empty? room) (db/delete room)))

(defn ws-leave-room [{:keys [connection-id] :as _request}]
  (with-lock
    (when-let [occupant (occupantc/by-conn-id connection-id)]
      (let [room (roomc/by-occupant occupant)
            room (roomc/remove-occupant! room occupant)]
        (push-room! room)
        (db/delete occupant)
        (maybe-delete-room room)))))