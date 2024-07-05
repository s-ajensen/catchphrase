(ns catchphrase.game-spec
  (:require [c3kit.apron.time :as time]
            [catchphrase.gamec :as gamec]
            [catchphrase.teamc :as teamc]
            [catchphrase.tf2 :as tf2]
            [catchphrase.dispatch :as dispatch]
            [catchphrase.game :as sut]
            [c3kit.bucket.api :as db]
            [c3kit.wire.apic :as apic]
            [speclj.core :refer :all]))

(declare ^:dynamic response)
(declare ^:dynamic game)

(describe "Game"
  (with-stubs)
  (tf2/init-with-schemas)

  (context "ws-fetch-game"
    (context "failure"
      (it "if occupant not found"
        (let [response (sut/ws-fetch-game {:connection-id "blah"})]
          (should= :fail (:status response))
          (should= "Occupant not found" (apic/flash-text response 0))))

      (it "if room not found"
        (let [response (sut/ws-fetch-game {:connection-id (:conn-id @tf2/spy)})]
          (should= :fail (:status response))
          (should= "Room not found" (apic/flash-text response 0)))))

    (context "success"
      (it "sends game with teams"
        (let [response (sut/ws-fetch-game {:connection-id (:conn-id @tf2/heavy)})
              [blu red] (teamc/by-game @tf2/ctf)]
          (should= :ok (:status response))
          (should= [@tf2/ctf blu red] (:payload response))))))

  (context "run-round!"

    (redefs-around [sut/sleep! (stub :sleep!)
                    dispatch/push-to-occupants! (stub :push-to-occupants!)])

    (before (gamec/start-round! @tf2/ctf))

    (it "waits for round to end"
      (sut/-run-round! @tf2/ctf @tf2/sawmill)
      (should-have-invoked :sleep! {:with [(:round-length @tf2/ctf)]}))

    (it "stops round"
      (sut/-run-round! @tf2/ctf @tf2/sawmill)
      (should= :round-end (:state @tf2/ctf)))

    #_(it "it assigns points to non-active team"
      (sut/-run-round! @tf2/ctf @tf2/sawmill)
      (let [[blu red] (teamc/by-game @tf2/ctf)]
        (should= (:id blu) (:active-team @tf2/ctf))
        (should= 1 (:points red))))

    (it "dispatches updated game & teams"
      (sut/-run-round! @tf2/ctf @tf2/sawmill)
      (let [[blu red] (teamc/by-game @tf2/ctf)]
        (should-have-invoked :push-to-occupants! {:with [(map db/entity (:occupants @tf2/sawmill))
                                                         :game/update
                                                         [@tf2/ctf]]}))))

  (context "ws-start-game"

    (redefs-around [dispatch/push-to-occupants! (stub :push-to-occupants!)
                    sut/run-round! (stub :run-round!)])

    (it "fails is connection-id is not host"
      (let [non-host @tf2/scout
            response (sut/ws-start-game {:connection-id (:conn-id non-host)})]
        (should= :fail (:status response))
        (should-be-nil (:payload response))
        (should= "Only the host can start the game!" (apic/flash-text response 0))))

    (context "succeeds"

      (with response (sut/ws-start-game {:connection-id (:conn-id @tf2/heavy)}))
      (with game (first (:payload @response)))

      (it "starts game"
        (should= :ok (:status @response))
        (should= :started (:state @game)))

      (it "with start time"
        (with-redefs [time/now (constantly (time/now))]
          (should= (time/now) (:round-start @game))
          (should= (time/now) (:round-start @tf2/ctf))))

      (it "with round length"
        (with-redefs [rand-int (stub :rand-int {:return 10})]
          (should= (time/seconds 50) (:round-length @game))
          (should= (time/seconds 50) (:round-length @tf2/ctf))))

      (it "with active occupant"
        (should= (:id @tf2/heavy) (:active-occupant @game)))

      (it "notifies occupants of game start"
        @response
        (should-have-invoked :push-to-occupants! {:with [(map db/entity (:occupants @tf2/sawmill))
                                                         :game/update
                                                         [@tf2/ctf]]}))
      (it "runs round"
        @response
        (should-have-invoked :run-round! {:with [@tf2/ctf @tf2/sawmill]}))))

  (context "ws-advance-game"
    (before (gamec/start-round! @tf2/ctf))
    (redefs-around [dispatch/push-to-occupants! (stub :push-to-occupants!)])

    (it "fails if occupant isn't the active occupant of the game"
      (let [non-active @tf2/scout
            response (sut/ws-advance-game {:connection-id (:conn-id non-active)})]
        (should= :fail (:status response))
        (should-be-nil (:payload response))
        (should= "You can only advance the game if it is your turn!" (apic/flash-text response 0))))

    (context "success"
      (it "updates the game's active occupant"
        (let [active @tf2/heavy
              old-game @tf2/ctf
              response (sut/ws-advance-game {:connection-id (:conn-id active)})]
          (should= :ok (:status response))
          (should= (assoc old-game :active-occupant (:id @tf2/medic)) @tf2/ctf)))

      (it "notifies occupants of new active occupant"
        (let [active @tf2/heavy
              old-game @tf2/ctf
              _response (sut/ws-advance-game {:connection-id (:conn-id active)})]
          (should-have-invoked :push-to-occupants! {:with [(map db/entity (:occupants @tf2/sawmill))
                                                           :game/update
                                                           [(assoc old-game :active-occupant (:id @tf2/medic))]]})))))

  (context "ws-steal-game"
    (before (-> @tf2/ctf gamec/start-round! gamec/advance-game! gamec/stop-round!))
    (redefs-around [dispatch/push-to-occupants! (stub :push-to-occupants!)])

    (it "fails if occupant isn't the active occupant of the game"
      (let [non-active @tf2/heavy
            response (sut/ws-steal-game {:connection-id (:conn-id non-active)})]
        (should= :fail (:status response))
        (should-be-nil (:payload response))
        (should= "You can only advance the game if it is your turn!" (apic/flash-text response 0))))

    (context "success"
      (it "increments other team's points"
        (let [active @tf2/medic
              old-blu (db/ffind-by :team :game (:id @tf2/ctf) :color :blu)
              response (sut/ws-steal-game {:connection-id (:conn-id active)})
              new-blu (db/ffind-by :team :game (:id @tf2/ctf) :color :blu)]
          (should= :ok (:status response))
          (should= (assoc old-blu :points 1) new-blu)))

      (it "notifies occupants of new points"
        (let [active @tf2/medic
              old-blu (db/ffind-by :team :game (:id @tf2/ctf) :color :blu)
              _response (sut/ws-steal-game {:connection-id (:conn-id active)})]
          (should-have-invoked :push-to-occupants! {:with [(map db/entity (:occupants @tf2/sawmill))
                                                           :game/update
                                                           [(assoc old-blu :points 1)]]})))))

  (context "ws-inc-counter"
    (context "failure"
      (it "if team not found"
        (let [response (sut/ws-inc-points {:connection-id (:conn-id @tf2/heavy)})]
          (should= :fail (:status response))
          (should= "Team not found" (apic/flash-text response 0)))))

    (context "success"
      (redefs-around [dispatch/push-to-connections! (stub :push-to-connections!)])

      (it "updates points of team"
        (let [response (sut/ws-inc-points {:connection-id (:conn-id @tf2/heavy)
                                           :params {:team (:active-team @tf2/ctf)}})]
          (should= :ok (:status response))
          (should= 1 (:points (db/entity (:active-team @tf2/ctf))))))

      (it "push to occupants"
        (let [response (sut/ws-inc-points {:connection-id (:conn-id @tf2/heavy)
                                           :params {:team (:active-team @tf2/ctf)}})]
          (should= :ok (:status response))
          (should-have-invoked :push-to-connections! {:with [(map (comp :conn-id db/entity) (:occupants @tf2/sawmill))
                                                             :game/update
                                                             [(db/entity (:active-team @tf2/ctf))]]})))

      (it "ends game if at 7 points"
        (db/tx (db/entity (:active-team @tf2/ctf)) :points 6)
        (let [response (sut/ws-inc-points {:connection-id (:conn-id @tf2/heavy)
                                           :params {:team (:active-team @tf2/ctf)}})]
          (should= :ok (:status response))
          (should= :over (:state @tf2/ctf))
          (should-have-invoked :push-to-connections! {:with [(map (comp :conn-id db/entity) (:occupants @tf2/sawmill))
                                                             :game/update
                                                             [(db/entity (:active-team @tf2/ctf))
                                                              @tf2/ctf]]}))))))
