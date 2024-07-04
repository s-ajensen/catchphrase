(ns catchphrase.occupantc_spec
  (:require [catchphrase.occupantc :as sut]
            [catchphrase.tf2 :as tf2]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [describe context it should=]]))

(describe "occupantc"
  (tf2/init-with-schemas)

  (it "constructor"
    (let [occupant (sut/->occupant "Lautrec" "conn-id")]
      (should= "Lautrec" (:nickname occupant))
      (should= "conn-id" (:conn-id occupant))))

  (context "create-occupant!"
    (it "assigns nickname"
      (sut/create-occupant! "Solaire")
      (should= "Solaire" (:nickname (sut/by-nickname "Solaire"))))

    (it "assigns conn-id"
      (sut/create-occupant! "Solaire" "conn-solaire")
      (should= "conn-solaire" (:conn-id (sut/by-nickname "Solaire")))))

  (context "or-id"
    (it "occupant"
      (should= 123 (sut/or-id {:id 123})))

    (it "id"
      (should= 123 (sut/or-id 123)))))