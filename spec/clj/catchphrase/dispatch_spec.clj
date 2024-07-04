(ns catchphrase.dispatch-spec
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.wire.websocket :as ws]
            [catchphrase.tf2 :as tf2 :refer [sawmill heavy medic scout]]
            [catchphrase.dispatch :as sut]
            [speclj.core :refer :all]))

(describe "Dispatch"
  (with-stubs)
  (tf2/init-with-schemas)

  (context "pushing"
    (redefs-around [ws/connected-ids (fn [] (set (map :conn-id [@heavy @medic @scout])))
                    ws/push! (stub :ws/push!)])

    (context "to member"
      (it "no connections"
        (with-redefs [ws/connected-ids (fn [] [])]
          @(sut/push-to-occupant! [@heavy] :some/method [@sawmill])
          (should-not-have-invoked :ws/push!)))

      (it "no members"
        @(sut/push-to-occupant! [] :some/method [@sawmill])
        (should-not-have-invoked :ws/push!))

      (it "one member and many connections"
        @(sut/push-to-occupant! @heavy :some/method [@sawmill])
        (should-have-invoked :ws/push! {:times 1})
        (should-have-invoked :ws/push! {:with ["conn-heavy" :some/method [@sawmill]]})))

    (context "to members"
      (it "no connections"
        (with-redefs [ws/connected-ids (fn [] [])]
          @(sut/push-to-occupants! [@heavy] :some/method [@sawmill])
          (should-not-have-invoked :ws/push!)))

      (it "no members"
        @(sut/push-to-occupants! [] :some/method [@sawmill])
        (should-not-have-invoked :ws/push!))

      (it "one member and many connections"
        @(sut/push-to-occupants! [@heavy] :some/method [@sawmill])
        (should-have-invoked :ws/push! {:times 1})
        (should-have-invoked :ws/push! {:with ["conn-heavy" :some/method [@sawmill]]}))

      (it "two members and many connections"
        @(sut/push-to-occupants! [@heavy @medic] :some/method [@sawmill])
        (should-have-invoked :ws/push! {:times 2})
        (should-have-invoked :ws/push! {:with ["conn-heavy" :some/method [@sawmill]]})
        (should-have-invoked :ws/push! {:with ["conn-medic" :some/method [@sawmill]]})))))