(ns catchphrase.game-roomc-spec
  (:require [c3kit.bucket.api :as db]
            [catchphrase.tf2 :as tf2 :refer [sawmill egypt heavy spy medic scout]]
            [catchphrase.roomc :as roomc]
            [catchphrase.game-roomc :as sut]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [describe context focus-it it should= should-not-contain
                                                              should-not-be-nil should-be-nil stub redefs-around with-stubs]]))

(describe "Game Room Common"
  (with-stubs)
  (tf2/init-with-schemas)

  (context "create-game-room!"

    (it "sets BLU to be the active team"
      (let [game-room (sut/create-game-room! "sawmill")
            game (db/entity (:game game-room))
            blu (db/ffind-by :team :game (:id game) :color :blu)]
        (should= (:id blu) (:active-team game))))

    (it "assigns 0 points for both teams"
      (let [game-room (sut/create-game-room! "sawmill")
            red (db/ffind-by :team :game (:game game-room) :color :red)
            blu (db/ffind-by :team :game (:game game-room) :color :blu)]
        (should= 0 (:points red))
        (should= 0 (:points blu))))))