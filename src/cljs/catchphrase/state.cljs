(ns catchphrase.state
  (:require [c3kit.bucket.api :as db]
            [catchphrase.occupant :as occupant]
            [catchphrase.page :as page]
            [reagent.core :as reagent]))

(def nickname (reagent/atom nil))

(def room-state (page/cursor [:room] {}))
(defn install-room! [code]
  (swap! room-state assoc :code code))
(def code (reagent/track #(:code @room-state)))
(def room (reagent/track #(db/ffind-by :room :code @code)))
(def occupants (reagent/track #(map db/entity (:occupants @room))))
(def game (reagent/track #(db/ffind :game)))
(def host? (reagent/track #(= @occupant/occupant-id (:id (first @occupants)))))