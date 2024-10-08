(ns catchphrase.schema.occupant-spec
  (:require [c3kit.apron.schema :as schema]
            [catchphrase.schema.occupant :as sut]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [describe it should=]]))

(describe "occupant schema"

  (it "nickname"
    (should= "must be present"
             (->> {:kind :room} (schema/validate sut/occupant) schema/error-message-map :nickname)))

  (it "conn-id"
    (should= "must be a string"
             (->> {:kind :room :conn-id :blah} (schema/validate sut/occupant) schema/error-message-map :conn-id))))