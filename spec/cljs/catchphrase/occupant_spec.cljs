(ns catchphrase.occupant-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]])
  (:require [catchphrase.dark-souls :as ds]
            [catchphrase.init :as init]
            [catchphrase.occupant :as sut]
            [c3kit.bucket.api :as db]
            [c3kit.wire.websocket :as ws]
            [reagent.core :as reagent]))

(describe "Occupant"
  (init/install-reagent-db-atom!)
  (init/install-legend!)
  (ds/with-schemas)
  (before (ds/init))

  (context "installs"
    (it "frampt"
      (sut/install! @ds/frampt)
      (should= @ds/frampt @sut/current)
      (should= (:nickname @ds/frampt) @sut/nickname))

    (it "lautrec"
      (sut/install! @ds/lautrec)
      (should= @ds/lautrec @sut/current)
      (should= (:nickname @ds/lautrec) @sut/nickname)))

  (it "clears"
    (sut/install! @ds/lautrec)
    (sut/clear!)
    (should-be-nil @sut/current))

  (context "receive-join!"
    (before (set! ws/client (reagent/atom {:connection {:id "conn-lautrec"}})))

    (it "transacts received entities"
      (let [lautrec @ds/lautrec]
        (db/clear)
        (should-be-nil (db/entity (:id lautrec)))
        (sut/receive-join! [lautrec])
        (should-not-be-nil (db/entity (:id lautrec)))))

    (it "install occupant"
      (let [lautrec @ds/lautrec]
        (db/clear)
        (should-be-nil @sut/current)
        (sut/receive-join! [lautrec])
        (should= lautrec @sut/current)))))