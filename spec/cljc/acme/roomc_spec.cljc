(ns acme.roomc-spec
  (:require [c3kit.bucket.api :as db]
            [acme.dark-souls :as ds :refer [firelink depths lautrec laurentius frampt patches]]
            [acme.roomc :as roomc]
            [acme.roomc :as sut]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [describe context focus-it it should= should-not-contain
                                                              should-not-be-nil should-be-nil stub redefs-around with-stubs]]))

(describe "roomc"
  (with-stubs)
  (ds/init-with-schemas)

  (context "create-room!"
    (it "assigns code"
      (sut/create-room! ds/firelink-code)
      (should= ds/firelink-code (:code (db/ffind-by :room :code ds/firelink-code)))))

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
      (sut/add-occupant! @depths @laurentius)
      (sut/add-occupant! @depths @frampt)
      (sut/add-occupant! @depths @patches)
      (should= (mapv :id [@laurentius @frampt @patches]) (:occupants @depths))))

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
      (sut/remove-occupant! @firelink @patches)
      (should-not-contain (:id @patches) (:occupants @firelink))))

  (it "finds room by occupant"
    (should= @firelink (roomc/by-occupant @lautrec))))