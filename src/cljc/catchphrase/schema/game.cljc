(ns catchphrase.schema.game
  (:require [c3kit.apron.schema :as s]))

(def game
  {:kind         (s/kind :game)
   :id           s/id

   :state           {:type :keyword}
   :active-team     {:type :ref}
   :round-start     {:type :instant}
   :round-length    {:type :long}
   :active-occupant {:type :ref}
   :counter         {:type :long}})

(def all [game])