(ns catchphrase.game-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]]
                   [c3kit.wire.spec-helperc :refer [should-have-invoked-ws should-not-select should-select]])
  (:require [catchphrase.gamec :as gamec]
            [catchphrase.occupant :as occupant]
            [catchphrase.state :as state]
            [catchphrase.tf2 :as tf2]
            [catchphrase.game :as sut]
            [catchphrase.init :as init]
            [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.spec-helper :as wire]
            [c3kit.wire.websocket :as ws]
            [speclj.stub :as stub]))

(describe "Game"
  (init/install-reagent-db-atom!)
  (init/install-legend!)
  (init/configure-api!)
  (with-stubs)
  (wire/stub-ws)
  (wire/with-root-dom)
  (tf2/init-with-schemas)
  (before (wire/render [sut/full]))

  (it "stucture"
    (should-select "#-game-container"))

  #_(it "displays counter"
    (should= "0" (wire/text "#-counter")))

  #_(it "submits counter increment"
    (wire/click! "#-inc-btn")
    (should-have-invoked-ws :game/inc-counter [] ccc/noop))

  (context "game update"
    (it "db/tx*'s params"
      (ws/push-handler {:kind :game/update :params [{:kind :team :color :yellow :game 0 :points 0}]})
      (wire/flush)
      (should-not-be-nil (db/ffind-by :team :color :yellow)))

    (it "fetches next word if game started"
      (db/tx @tf2/ctf :state :round-end)
      (ws/push-handler {:kind :game/update :params [(assoc @tf2/ctf :state :started)]})
      (should-have-invoked :ws/call! {:with [:word/next-word nil sut/receive-new-word]}))

    (it "doesn't fetch next word if not round end"
      (ws/push-handler {:kind :game/update :params [(assoc @tf2/ctf :state :lobby)]})
      (should-not-have-invoked :ws/call!)))

  (context "receive new word"
    (it "updates ratom"
      (sut/receive-new-word "greetings")
      (should= "greetings" @sut/current-word)
      (sut/receive-new-word "hi")
      (should= "hi" @sut/current-word)))

  (context "room"
    (before (state/install-room! (:code @tf2/sawmill))
            (occupant/install! @tf2/heavy)
            (wire/flush))

    (context "displays points"
      (it "blu"
        (should= "POINTS: 0" (wire/html (str "#-blu-pts")))
        (db/tx @sut/blu :points 1)
        (wire/flush)
        (should= "POINTS: 1" (wire/html (str "#-blu-pts"))))

      (it "red"
        (should= "POINTS: 0" (wire/html (str "#-red-pts")))
        (db/tx @sut/red :points 2)
        (wire/flush)
        (should= "POINTS: 2" (wire/html (str "#-red-pts")))))

    (context "displays teams"
      (it "blu"
        (should= "heavy (Host)" (wire/html (str "#-blu-" (:id @tf2/heavy))))
        (should= "scout" (wire/html (str "#-blu-" (:id @tf2/scout)))))

      (it "red"
        (should= "medic" (wire/html (str "#-red-" (:id @tf2/medic))))))

    (it "displays game"
      (should-select "#-game-container")))

  (context "start game button"
    (before (state/install-room! (:code @tf2/sawmill)))

    (it "displays for host"
      (occupant/install! @tf2/heavy)
      (wire/flush)
      (should-select "#-start-button"))

    (it "doesn't display for non-host"
      (occupant/install! @tf2/medic)
      (wire/flush)
      (should-not-select "#-start-button"))

    (it "starts the game"
      (occupant/install! @tf2/heavy)
      (wire/flush)
      (wire/click! "#-start-button")
      (should-have-invoked :ws/call! {:with [:game/start nil db/tx*]}))

    (it "fetches new word"
      (occupant/install! @tf2/heavy)
      (wire/flush)
      (wire/click! "#-start-button")
      (should-have-invoked :ws/call! {:with [:word/next-word nil sut/receive-new-word]})))

  (context "current phrase"
    (before (occupant/install! @tf2/heavy)
            (gamec/start-round! @tf2/ctf)
            (reset! sut/current-word "it")
            (wire/flush))

    (it "displays for active user"
      (should= "it" (wire/html "#-phrase")))

    (it "doesn't display for non-active user"
      (occupant/install! @tf2/medic)
      (wire/flush)
      (should-not-select  "#-phrase")))

  (context "game not started"
    (context "game state display"
      (it "displays message"
        (should= "Waiting for host to start the round..." (wire/text "#-game-state"))))

    (it "can't see advance button"
      (should-not-select "#-advance-button"))

    (it "can't see new word button"
      (should-not-select "#-new-word-button")))

  (context "game started"
    (before (occupant/install! @tf2/heavy)
            (gamec/start-round! @tf2/ctf)
            (wire/flush))

    (it "doesn't display start game button"
      (should-not-select "#-start-button"))

    (it "doesn't display point buttons"
      (should-not-select "#-blu-pts-button")
      (should-not-select "#-red-pts-button"))

    (context "game state display"
      (it "displays first occupant's turn"
        (should= "It's heavy's turn! Time is ticking!" (wire/text "#-game-state")))

      (it "displays next occupant's turn"
        (gamec/advance-game! @tf2/ctf)
        (wire/flush)
        (should= "It's medic's turn! Time is ticking!" (wire/text "#-game-state"))))

    (context "state label"
      (context "active occupant"
        (it "can see advance button"
          (should-select "#-advance-button"))

        (it "can click advance button"
          (stub/clear!)
          (wire/click! "#-advance-button")
          (should-have-invoked :ws/call! {:with [:game/advance nil ccc/noop]})
          (should-have-invoked :ws/call! {:with [:word/next-word nil sut/receive-new-word]}))

        (it "can see new word button"
          (should-select "#-new-word-button"))

        (it "can click new word button"
          (stub/clear!)
          (wire/click! "#-new-word-button")
          (should-have-invoked :ws/call! {:with [:word/next-word nil sut/receive-new-word]})))

      (context "non-active occupant"
        (it "can't see advance button"
          (occupant/install! @tf2/medic)
          (wire/flush)
          (should-not-select "#-advance-button"))

        (it "can't see new word button"
          (occupant/install! @tf2/medic)
          (wire/flush)
          (should-not-select "#-new-word-button")))))

  (context "game ended"
    (before (-> @tf2/ctf gamec/start-round! gamec/advance-game! gamec/stop-round!)
            (wire/flush))

    (it "displays message"
      (should= "Round ended..." (wire/text "#-game-state")))

    (it "displays start button"
      (occupant/install! @tf2/heavy)
      (wire/flush)
      (should-select "#-start-button"))

    (context "point buttons"
      (it "displays for active occupant"
        (occupant/install! @tf2/medic)
        (wire/flush)
        (should-select "#-blu-pts-button")
        (should-select "#-red-pts-button"))

      (it "increments points"
        (occupant/install! @tf2/medic)
        (wire/flush)
        (wire/click! "#-blu-pts-button")
        (should-have-invoked :ws/call! {:with [:game/inc-points {:team (:team @tf2/heavy)} ccc/noop]})
        (wire/click! "#-red-pts-button")
        (should-have-invoked :ws/call! {:with [:game/inc-points {:team (:team @tf2/scout)} ccc/noop]}))

      (it "doesn't display for non-active occupant"
        (occupant/install! @tf2/scout)
        (wire/flush)
        (should-not-select "#-blu-pts-button")
        (should-not-select "#-red-pts-button")))

    (it "displays start button for host"
      (occupant/install! @tf2/heavy)
      (wire/flush)
      (should-select "#-start-button")))

  (context "game over"
    (before (occupant/install! @tf2/heavy)
            (gamec/start-round! @tf2/ctf)
            (db/tx @tf2/ctf :state :over)
            (wire/flush))

    (it "displays message"
      (should= "Game Over!" (wire/html "#-game-state")))

    (it "hides phrase"
      (should-not-select "#-phrase"))))