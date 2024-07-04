(ns catchphrase.dispatch-spec
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.wire.websocket :as ws]
            [catchphrase.tf2 :as tf2 :refer [firelink lautrec frampt patches]]
            [catchphrase.dispatch :as sut]
            [speclj.core :refer :all]))

(describe "Dispatch"
  (with-stubs)
  (tf2/init-with-schemas)

  (context "pushing"
    (redefs-around [ws/connected-ids (fn [] (set (map :conn-id [@lautrec @frampt @patches])))
                    ws/push! (stub :ws/push!)])

    (context "to member"
      (it "no connections"
        (with-redefs [ws/connected-ids (fn [] [])]
          @(sut/push-to-occupant! [@lautrec] :some/method [@firelink])
          (should-not-have-invoked :ws/push!)))

      (it "no members"
        @(sut/push-to-occupant! [] :some/method [@firelink])
        (should-not-have-invoked :ws/push!))

      (it "one member and many connections"
        @(sut/push-to-occupant! @lautrec :some/method [@firelink])
        (should-have-invoked :ws/push! {:times 1})
        (should-have-invoked :ws/push! {:with ["conn-lautrec" :some/method [@firelink]]})))

    (context "to members"
      (it "no connections"
        (with-redefs [ws/connected-ids (fn [] [])]
          @(sut/push-to-occupants! [@lautrec] :some/method [@firelink])
          (should-not-have-invoked :ws/push!)))

      (it "no members"
        @(sut/push-to-occupants! [] :some/method [@firelink])
        (should-not-have-invoked :ws/push!))

      (it "one member and many connections"
        @(sut/push-to-occupants! [@lautrec] :some/method [@firelink])
        (should-have-invoked :ws/push! {:times 1})
        (should-have-invoked :ws/push! {:with ["conn-lautrec" :some/method [@firelink]]}))

      (it "two members and many connections"
        @(sut/push-to-occupants! [@lautrec @frampt] :some/method [@firelink])
        (should-have-invoked :ws/push! {:times 2})
        (should-have-invoked :ws/push! {:with ["conn-lautrec" :some/method [@firelink]]})
        (should-have-invoked :ws/push! {:with ["conn-frampt" :some/method [@firelink]]})))))