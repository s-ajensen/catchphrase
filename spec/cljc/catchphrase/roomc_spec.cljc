(ns catchphrase.roomc-spec
  (:require [c3kit.bucket.api :as db]
            [catchphrase.tf2 :as tf2 :refer [sawmill egypt heavy spy medic scout]]
            [catchphrase.roomc :as roomc]
            [catchphrase.roomc :as sut]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [describe context focus-it it should= should-not-contain
                                                              should-not-be-nil should-be-nil stub redefs-around with-stubs]]))

(describe "roomc"
  (with-stubs)
  (tf2/init-with-schemas)

  (context "create-room!"
    (it "assigns code"
      (sut/create-room! tf2/sawmill-code)
      (should= tf2/sawmill-code (:code (db/ffind-by :room :code tf2/sawmill-code)))))

  (context "add-occupant"
    (it "to empty room"
      (let [room (sut/add-occupant {:occupants []} {:id 123})]
        (should= [123] (:occupants room))))

    (it "to room with one occupant"
      (let [room (sut/add-occupant {:occupants [123]} 124)]
        (should= [123 124] (:occupants room))))

    (it "to room with many occupants"
      (let [room (sut/add-occupant {:occupants [123 124]} 125)]
        (should= [123 124 125] (:occupants room)))))

  (context "join-room!"
    (it "stores users who have joined in order"
      (sut/add-occupant! @egypt @spy)
      (sut/add-occupant! @egypt @medic)
      (sut/add-occupant! @egypt @scout)
      (should= (mapv :id [@spy @medic @scout]) (:occupants @egypt))))

  (context "remove-occupant"
    (it "from empty room"
      (let [room (sut/remove-occupant {:occupants []} {:id 123})]
        (should= [] (:occupants room))))

    (it "from room with one occupant"
      (let [room (sut/remove-occupant {:occupants [123]} 123)]
        (should= [] (:occupants room))))

    (it "from room with many occupants"
      (let [room (sut/remove-occupant {:occupants [123 124 125]} 123)]
        (should= [124 125] (:occupants room)))))

  (context "remove-occupant!"
    (it "removes occupant from room"
      (sut/remove-occupant! @sawmill @scout)
      (should-not-contain (:id @scout) (:occupants @sawmill))))

  (it "finds room by occupant"
    (should= @sawmill (roomc/by-occupant @heavy))))