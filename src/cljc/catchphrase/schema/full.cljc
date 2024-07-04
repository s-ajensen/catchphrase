(ns catchphrase.schema.full
  (:require [catchphrase.schema.room :as room]
            [catchphrase.schema.occupant :as occupant]
            [catchphrase.schema.game :as game]
            [catchphrase.schema.game-room :as game-room]))

(def full-schema
  (concat room/all
          occupant/all
          game/all
          game-room/all))
