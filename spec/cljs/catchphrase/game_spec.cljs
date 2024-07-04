(ns catchphrase.game-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]]
                   [c3kit.wire.spec-helperc :refer [should-have-invoked-ws should-not-select should-select]])
  (:require [catchphrase.tf2 :as tf2]
            [catchphrase.game :as sut]
            [catchphrase.init :as init]
            [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.spec-helper :as wire]
            [c3kit.wire.websocket :as ws]))

(describe "Game"
  (init/install-reagent-db-atom!)
  (init/install-legend!)
  (init/configure-api!)
  (with-stubs)
  (wire/stub-ws)
  (wire/with-root-dom)
  (tf2/with-schemas)
  (before (db/set-safety! false)
          (tf2/init)
          (wire/render [sut/full tf2/ctf]))

  (it "stucture"
    (should-select "#-game-container"))

  (it "displays counter"
    (should= "0" (wire/text "#-counter")))

  (it "submits counter increment"
    (wire/click! "#-inc-btn")
    (should-have-invoked-ws :game/inc-counter [] ccc/noop))

  (it "receives game update"
    (ws/push-handler {:kind :game/update :params [(update @tf2/ctf :counter inc)]})
    (wire/flush)
    (should= "1" (wire/text "#-counter"))))