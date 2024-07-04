(ns acme.dispatch
  (:require [c3kit.wire.websocket :as ws]
            [clojure.set :as set]))

(defn- push-to-connections! [conn-ids method data]
  (future
    (doseq [uid (set/intersection (ws/connected-ids) (set conn-ids))]
      (ws/push! uid method data))))

(defn push-to-occupant! [occupant method data]
  (push-to-connections! [(:conn-id occupant)] method data))

(defn push-to-occupants! [occupants method data]
  (push-to-connections! (map :conn-id occupants) method data))