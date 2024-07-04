(ns catchphrase.schema.game-room
  (:require [c3kit.apron.schema :as s]))

(def game-room
  {:kind (s/kind :game-room)
   :id   s/id
   :game {:type :ref}
   :room {:type :ref}})

(def all [game-room])