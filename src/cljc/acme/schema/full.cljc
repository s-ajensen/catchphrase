(ns acme.schema.full
  (:require [acme.schema.room :as room]
            [acme.schema.occupant :as occupant]
            [acme.schema.game :as game]
            [acme.schema.game-room :as game-room]))

(def full-schema
  (concat room/all
          occupant/all
          game/all
          game-room/all))
