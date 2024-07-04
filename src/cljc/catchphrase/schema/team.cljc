(ns catchphrase.schema.team
  (:require [c3kit.apron.schema :as s]))

(def team
  {:kind      (s/kind :team)
   :id        s/id
   :color     {:type :keyword}
   :game      {:type :ref}
   :points    {:type :long}})

(def all [team])