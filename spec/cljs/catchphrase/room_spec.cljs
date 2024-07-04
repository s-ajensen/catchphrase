(ns catchphrase.room-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]]
                   [c3kit.wire.spec-helperc :refer [should-not-select should-select]])
  (:require [catchphrase.occupant :as occupant]
            [c3kit.apron.corec :as ccc]
            [catchphrase.tf2 :as tf2]
            [catchphrase.init :as init]
            [catchphrase.page :as page]
            [catchphrase.room :as sut]
            [catchphrase.roomc :as roomc]
            [c3kit.wire.spec-helper :as wire]
            [c3kit.bucket.api :as db]
            [catchphrase.state :as state]
            [catchphrase.layout :as layout]
            [catchphrase.routes :as routes]
            [c3kit.wire.websocket :as ws]
            [speclj.stub :as stub]))

(defn load-room! [{:keys [code] :as _room}]
  (state/install-room! code)
  (routes/load-page! :room)
  (wire/flush))

(describe "Room"
  (init/install-reagent-db-atom!)
  (init/install-legend!)
  (init/configure-api!)
  (with-stubs)
  (wire/stub-ws)
  (wire/with-root-dom)
  (tf2/with-schemas)
  (before (db/set-safety! false)
          (db/clear)
          (tf2/init)
          (occupant/clear!)
          (wire/render [layout/default]))

  (context "on enter"
    (before (routes/load-page! nil))

    (it "fetches room"
      (load-room! @tf2/sawmill)
      (should-have-invoked :ws/call! {:with [:room/fetch {:room-code tf2/sawmill-code} db/tx*]}))

    (it "joins room if non-blank nickname"
      (reset! state/nickname "Hello")
      (load-room! @tf2/sawmill)
      (should-have-invoked :ws/call! {:with [:room/join
                                             {:nickname "Hello" :room-code tf2/sawmill-code}
                                             occupant/receive-join!]}))

    (it "doesn't join room if blank nickname"
      (reset! state/nickname " ")
      (load-room! @tf2/sawmill)
      (should-not-have-invoked :ws/call! {:with [:room/join
                                                 {:nickname " " :room-code tf2/sawmill-code}
                                                 occupant/receive-join!]}))

    (it "deletes rooms"
      (load-room! @tf2/sawmill)
      (should= [] (db/find :room)))

    (it "deletes games"
      (load-room! @tf2/sawmill)
      (should= [] (db/find :game))))

  (context "on exit"
    (before (page/exiting! :room))

    (it "calls room/leave"
      (should-have-invoked :ws/call! {:with [:room/leave {} ccc/noop]}))

    (it "resets room-state"
      (should= {} @state/room-state)))

  (context "maybe not found"
    (it "renders not found if no room"
      (state/install-room! nil)
      (wire/flush)
      (should-select "#-not-found"))

    (it "renders prompt or room if room"
      (load-room! @tf2/sawmill)
      (wire/flush)
      (should-select "#-prompt-or-room")))

  (context "existing room"
    (before (load-room! @tf2/sawmill))

    (context "nickname prompt or room"
      (it "renders nickname prompt if no nickname"
        (should-select "#-nickname-prompt")
        (should-not-select "#-room"))

      (it "renders room if nickname"
        (stub/clear!)
        (occupant/install! @tf2/medic)
        (wire/flush)
        (should-have-invoked :ws/call! {:with [:game/fetch nil db/tx*]})
        (should-not-select "#-nickname-prompt")
        (should-select "#-room")))

    (context "nickname prompt"
      (it "updates input on change"
        (wire/change! "#-nickname-input" "heavy")
        (should= "heavy" (wire/value "#-nickname-input"))
        (wire/change! "#-nickname-input" "scout")
        (should= "scout" (wire/value "#-nickname-input")))

      (context "button click"
        (it "joins room"
          (wire/change! "#-nickname-input" "heavy")
          (wire/click! "#-join-button")
          (should-have-invoked :ws/call! {:with [:room/join
                                                 {:nickname "heavy" :room-code tf2/sawmill-code}
                                                 occupant/receive-join!]}))

        (it "doesn't join room if blank nickname"
          (wire/change! "#-nickname-input" " ")
          (wire/click! "#-join-button")
          (should-not-have-invoked :ws/call!))))

    (context "room"
      (before (occupant/install! @tf2/heavy)
              (wire/flush))

      (context "displays teams"
        (it "blu"
          (should= "heavy" (wire/html (str "#-blu-" (:id @tf2/heavy))))
          (should= "scout" (wire/html (str "#-blu-" (:id @tf2/scout)))))

        (it "red"
          (should= "medic" (wire/html (str "#-red-" (:id @tf2/medic))))))

      (it "displays game"
        (should-select "#-game-container"))))

  (it "receives room update"
    (ws/push-handler {:kind :room/update :params [(roomc/->room "Greetings")]})
    (should-have-invoked :ws/call! {:with [:game/fetch nil db/tx*]})
    (should-not-be-nil (db/ffind-by :room :code "Greetings"))))