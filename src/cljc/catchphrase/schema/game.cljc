(ns catchphrase.schema.game
  (:require [c3kit.apron.schema :as s]))

(def game
  {:kind         (s/kind :game)
   :id           s/id

   :round-start  {:type :instant}
   :round-length {:type :long}

   :counter      {:type :long}})

(def all [game])