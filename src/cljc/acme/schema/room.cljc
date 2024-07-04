(ns acme.schema.room
  (:require [c3kit.apron.schema :as s]))

(def room
  {:kind         (s/kind :room)
   :id           s/id
   :code         {:type :string :validate s/present? :message "must be present"}
   :occupants    {:type [:long] :validate s/present? :message "must be present"}
   })

(def all [room])