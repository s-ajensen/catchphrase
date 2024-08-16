(ns catchphrase.gamec-spec
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]
            [catchphrase.gamec :as sut]
            [catchphrase.roomc :as roomc]
            [catchphrase.tf2 :as tf2]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [focus-context before describe context it should=]]))

(describe "gamec"
  (tf2/init-with-schemas)

  (context "advance-game!"
    (before (db/delete @tf2/sawmill)
            (db/tx* (map #(dissoc % :team) [@tf2/heavy @tf2/scout @tf2/medic @tf2/spy @tf2/demo])))

    (it "for game with 1 occupant"
      (roomc/join-room! @tf2/egypt @tf2/heavy)
      (should= (:id @tf2/heavy) (:active-occupant (sut/advance-game! @tf2/cp))))

    (it "for game with 2 occupants"
      (roomc/join-room! @tf2/egypt @tf2/heavy)
      (roomc/join-room! @tf2/egypt @tf2/scout)
      (sut/start-round! @tf2/cp)
      (should= (:id @tf2/scout) (:active-occupant (sut/advance-game! @tf2/cp))))

    (it "for game with 3 occupants"
      (roomc/join-room! @tf2/egypt @tf2/heavy)
      (roomc/join-room! @tf2/egypt @tf2/scout)
      (roomc/join-room! @tf2/egypt @tf2/medic)
      (sut/start-round! @tf2/cp)
      (sut/advance-game! @tf2/cp)
      (should= (:id @tf2/medic) (:active-occupant (sut/advance-game! @tf2/cp))))

    (it "for game with 4 occupants"
      (roomc/join-room! @tf2/egypt @tf2/heavy)
      (roomc/join-room! @tf2/egypt @tf2/scout)
      (roomc/join-room! @tf2/egypt @tf2/medic)
      (roomc/join-room! @tf2/egypt @tf2/spy)
      (sut/start-round! @tf2/cp)
      (sut/advance-game! @tf2/cp)
      (sut/advance-game! @tf2/cp)
      (should= (:id @tf2/spy) (:active-occupant (sut/advance-game! @tf2/cp))))

    (it "for game with 5 occupants"
      (roomc/join-room! @tf2/egypt @tf2/heavy)
      (roomc/join-room! @tf2/egypt @tf2/scout)
      (roomc/join-room! @tf2/egypt @tf2/medic)
      (roomc/join-room! @tf2/egypt @tf2/spy)
      (roomc/join-room! @tf2/egypt @tf2/demo)
      (sut/start-round! @tf2/cp)
      (sut/advance-game! @tf2/cp)
      (sut/advance-game! @tf2/cp)
      (sut/advance-game! @tf2/cp)
      (should= (:id @tf2/demo) (:active-occupant (sut/advance-game! @tf2/cp))))

    (it "cycles after everyone plays"
      (roomc/join-room! @tf2/egypt @tf2/heavy)
      (roomc/join-room! @tf2/egypt @tf2/scout)
      (sut/start-round! @tf2/cp)
      (sut/advance-game! @tf2/cp)
      (should= (:id @tf2/heavy) (:active-occupant (sut/advance-game! @tf2/cp))))))
