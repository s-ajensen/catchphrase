(ns catchphrase.schema.room-spec
  (:require [c3kit.apron.schema :as schema]
            [catchphrase.schema.room :as sut]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [describe it should=]]))

(describe "room schema"

  (it "code is required"
    (should= "must be present"
      (->> {:kind :room} (schema/validate sut/room) schema/error-message-map :code)))

  (it "occupants is required"
    (should= "must be present"
             (->> {:kind :room}
                  (schema/validate sut/room)
                  schema/error-message-map
                  :occupants))))