(ns acme.occupantc
  (:require [c3kit.bucket.api :as db]))

(defn ->occupant
  ([nickname]
   {:kind     :occupant
    :nickname nickname})
  ([nickname conn-id]
   (merge (->occupant nickname)
          {:conn-id conn-id})))

(defn create-occupant!
  ([nickname] (db/tx (->occupant nickname)))
  ([nickname conn-id] (db/tx (->occupant nickname conn-id))))

(defn or-id [occupant-or-id]
  ((some-fn :id identity) occupant-or-id))

(defn by-nickname [nickname]
  (db/ffind-by :occupant :nickname nickname))
(defn by-conn-id [conn-id]
  (db/ffind-by :occupant :conn-id conn-id))