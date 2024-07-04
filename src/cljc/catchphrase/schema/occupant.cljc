(ns catchphrase.schema.occupant
  (:require [c3kit.apron.schema :as s]))

(def occupant
  {:kind     (s/kind :occupant)
   :id       s/id
   :nickname {:type :string :validate s/present? :message "must be present"}
   :conn-id  {:type :string :message "must be a string"}
   :team     {:type :ref}
   })

(def all [occupant])