(ns catchphrase.home-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]])
  (:require [accountant.core :as accountant]
            [catchphrase.dark-souls :as ds]
            [catchphrase.home :as sut]
            [catchphrase.layout :as layout]
            [catchphrase.routes :as routes]
            [c3kit.wire.spec-helper :as wire]
            [catchphrase.state :as state]))

(defn stub-navigate! []
  (redefs-around [accountant/navigate! (stub :navigate!)]))

(describe "Home"
  (with-stubs)
  (wire/stub-ws)
  (stub-navigate!)
  (wire/with-root-dom)
  (ds/with-schemas)
  (before (reset! state/nickname nil)
          (wire/render [layout/default])
          (routes/load-page! :home))

  (context "joins room with code and nickname"
    (it "UK2LLJ, Lautrec"
      (reset! state/nickname "Lautrec")
      (sut/navigate-to-room! ["UK2LLJ"])
      (should-have-invoked :navigate! {:with ["/room/UK2LLJ"]})))

  (context "nickname input"
    (it "updates value on change"
      (wire/change! "#-nickname-input" "Lautrec")
      (should= "Lautrec" (wire/value "#-nickname-input"))
      (wire/change! "#-nickname-input" "Patches")
      (should= "Patches" (wire/value "#-nickname-input"))))

  (context "create room"
    (it "does nothing if no nickname"
      (wire/click! "#-create-room-button")
      (should-not-have-invoked :ws/call!))

    (it "does nothing if blank nickname"
      (wire/change! "#-nickname-input" " ")
      (wire/click! "#-create-room-button")
      (should-not-have-invoked :ws/call!))))