(ns acme.room-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]]
                   [c3kit.wire.spec-helperc :refer [should-not-select should-select]])
  (:require [acme.occupant :as occupant]
            [c3kit.apron.corec :as ccc]
            [acme.dark-souls :as ds]
            [acme.init :as init]
            [acme.page :as page]
            [acme.room :as sut]
            [acme.roomc :as roomc]
            [c3kit.wire.spec-helper :as wire]
            [c3kit.bucket.api :as db]
            [acme.state :as state]
            [acme.layout :as layout]
            [acme.routes :as routes]
            [c3kit.wire.websocket :as ws]
            [speclj.stub :as stub]))

(defn load-room! [{:keys [code] :as _room}]
  (sut/install-room! code)
  (routes/load-page! :room)
  (wire/flush))

(describe "Room"
  (init/install-reagent-db-atom!)
  (init/install-legend!)
  (init/configure-api!)
  (with-stubs)
  (wire/stub-ws)
  (wire/with-root-dom)
  (ds/with-schemas)
  (before (db/set-safety! false)
          (db/clear)
          (ds/init)
          (occupant/clear!)
          (wire/render [layout/default]))

  (context "on enter"
    (before (routes/load-page! nil))

    (it "fetches room"
      (load-room! @ds/firelink)
      (should-have-invoked :ws/call! {:with [:room/fetch {:room-code ds/firelink-code} db/tx*]}))

    (it "joins room if non-blank nickname"
      (reset! state/nickname "Hello")
      (load-room! @ds/firelink)
      (should-have-invoked :ws/call! {:with [:room/join
                                             {:nickname "Hello" :room-code ds/firelink-code}
                                             occupant/receive-join!]}))

    (it "doesn't join room if blank nickname"
      (reset! state/nickname " ")
      (load-room! @ds/firelink)
      (should-not-have-invoked :ws/call! {:with [:room/join
                                                 {:nickname " " :room-code ds/firelink-code}
                                                 occupant/receive-join!]}))

    (it "deletes rooms"
      (load-room! @ds/firelink)
      (should= [] (db/find :room)))

    (it "deletes games"
      (load-room! @ds/firelink)
      (should= [] (db/find :game))))

  (context "on exit"
    (before (page/exiting! :room))

    (it "calls room/leave"
      (should-have-invoked :ws/call! {:with [:room/leave {} ccc/noop]}))

    (it "resets room-state"
      (should= {} @sut/room-state)))

  (context "maybe not found"
    (it "renders not found if no room"
      (sut/install-room! nil)
      (wire/flush)
      (should-select "#-not-found"))

    (it "renders prompt or room if room"
      (load-room! @ds/firelink)
      (wire/flush)
      (should-select "#-prompt-or-room")))

  (context "existing room"
    (before (load-room! @ds/firelink))

    (context "nickname prompt or room"
      (it "renders nickname prompt if no nickname"
        (should-select "#-nickname-prompt")
        (should-not-select "#-room"))

      (it "renders room if nickname"
        (stub/clear!)
        (occupant/install! @ds/frampt)
        (wire/flush)
        (should-have-invoked :ws/call! {:with [:game/fetch nil db/tx]})
        (should-not-select "#-nickname-prompt")
        (should-select "#-room")))

    (context "nickname prompt"
      (it "updates input on change"
        (wire/change! "#-nickname-input" "Lautrec")
        (should= "Lautrec" (wire/value "#-nickname-input"))
        (wire/change! "#-nickname-input" "Patches")
        (should= "Patches" (wire/value "#-nickname-input")))

      (context "button click"
        (it "joins room"
          (wire/change! "#-nickname-input" "Lautrec")
          (wire/click! "#-join-button")
          (should-have-invoked :ws/call! {:with [:room/join
                                                 {:nickname "Lautrec" :room-code ds/firelink-code}
                                                 occupant/receive-join!]}))

        (it "doesn't join room if blank nickname"
          (wire/change! "#-nickname-input" " ")
          (wire/click! "#-join-button")
          (should-not-have-invoked :ws/call!))))

    (context "room"
      (before (occupant/install! @ds/lautrec)
              (wire/flush))

      (context "displays occupants"

        (it "with one occupant"
          (let [lautrec @ds/lautrec]
            (run! db/delete (db/find :occupant))
            (db/tx lautrec)
            (wire/flush)
            (should= "Lautrec" (wire/html (str "#-occupant-" (:id lautrec))))))

        (it "with multiple occupants"
          (should= "Lautrec" (wire/html (str "#-occupant-" (:id @ds/lautrec-atom))))
          (should= "Kingseeker Frampt" (wire/html (str "#-occupant-" (:id @ds/frampt-atom))))
          (should= "Patches" (wire/html (str "#-occupant-" (:id @ds/patches))))))

      (it "displays game"
        (should-select "#-game-container"))))

  (it "receives room update"
    (ws/push-handler {:kind :room/update :params [(roomc/->room "Greetings")]})
    (should-not-be-nil (db/ffind-by :room :code "Greetings"))))