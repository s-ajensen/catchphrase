(ns catchphrase.occupant-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]])
  (:require [catchphrase.tf2 :as tf2]
            [catchphrase.init :as init]
            [catchphrase.occupant :as sut]
            [c3kit.bucket.api :as db]
            [c3kit.wire.websocket :as ws]
            [reagent.core :as reagent]))

(describe "Occupant"
  (init/install-reagent-db-atom!)
  (init/install-legend!)
  (tf2/with-schemas)
  (before (tf2/init))

  (context "installs"
    (it "frampt"
      (sut/install! @tf2/medic)
      (should= @tf2/medic @sut/current)
      (should= (:nickname @tf2/medic) @sut/nickname))

    (it "heavy"
      (sut/install! @tf2/heavy)
      (should= @tf2/heavy @sut/current)
      (should= (:nickname @tf2/heavy) @sut/nickname)))

  (it "clears"
    (sut/install! @tf2/heavy)
    (sut/clear!)
    (should-be-nil @sut/current))

  (context "receive-join!"
    (before (set! ws/client (reagent/atom {:connection {:id "conn-heavy"}})))

    (it "transacts received entities"
      (let [heavy @tf2/heavy]
        (db/clear)
        (should-be-nil (db/entity (:id heavy)))
        (sut/receive-join! [heavy])
        (should-not-be-nil (db/entity (:id heavy)))))

    (it "install occupant"
      (let [heavy @tf2/heavy]
        (db/clear)
        (should-be-nil @sut/current)
        (sut/receive-join! [heavy])
        (should= heavy @sut/current)))))