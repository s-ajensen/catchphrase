(ns catchphrase.gamec-spec
  (:require [catchphrase.gamec :as sut]
            [catchphrase.tf2 :as tf2]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [describe context it should=]]))

(describe "gamec"
  (tf2/init-with-schemas)

  (context "next-occupant"
    (it "returns first occupant if no current occupant"
      (should= @tf2/heavy (sut/next-occupant nil [@tf2/heavy @tf2/medic @tf2/scout])))

    (it "returns second occupant if on different team"
      (should= @tf2/medic (sut/next-occupant @tf2/heavy [@tf2/heavy @tf2/medic @tf2/scout])))

    (it "returns next occupant that is on different team"
      (should= @tf2/scout (sut/next-occupant @tf2/medic [@tf2/heavy @tf2/medic @tf2/scout])))

    (it "cycles through occupants"
      (should= @tf2/medic (sut/next-occupant @tf2/scout [@tf2/heavy @tf2/medic @tf2/scout])))))
