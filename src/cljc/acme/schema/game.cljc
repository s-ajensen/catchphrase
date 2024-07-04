(ns acme.schema.game
  (:require [c3kit.apron.schema :as s]))

(def game
  {:kind    (s/kind :game)
   :id      s/id
   :counter {:type :long}})

(def all [game])