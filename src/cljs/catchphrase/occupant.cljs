(ns catchphrase.occupant
  (:require [c3kit.bucket.api :as db]
            [c3kit.wire.websocket :as ws]
            [reagent.core :as reagent]))

(def occupant-id (reagent/atom nil))
(def current (reagent/track #(db/entity :occupant @occupant-id)))
(def nickname (reagent/cursor current [:nickname]))

(defn install! [occupant] (reset! occupant-id (:id occupant occupant)))
(defn clear! [] (reset! occupant-id nil))

(defn receive-join! [response]
  (db/tx* response)
  (install! (db/ffind-by :occupant :conn-id (:id (:connection @ws/client)))))