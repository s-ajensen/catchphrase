(ns catchphrase.game-spec
  (:require [c3kit.apron.time :as time]
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

    (before (sut/start-round! @tf2/ctf))

    (it "waits for round to end"
      (sut/-run-round! @tf2/ctf)
      (should-have-invoked :sleep! {:with [(:round-length @tf2/ctf)]}))

    (it "stops round"
      (sut/-run-round! @tf2/ctf)
      (should= :round-end (:state @tf2/ctf)))

    (it "it assigns points to non-active team"
      (sut/-run-round! @tf2/ctf)
      (let [[blu red] (teamc/by-game @tf2/ctf)]
        (should= (:id blu) (:active-team @tf2/ctf))
        (should= 1 (:points red)))))

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

      (it "notifies occupants of game start"
        @response
        (should-have-invoked :push-to-occupants! {:with [(map db/entity (:occupants @tf2/sawmill))
                                                         :game/update
                                                         [@tf2/ctf]]}))
      (it "runs round"
        @response
        (should-have-invoked :run-round! {:with [@tf2/ctf]}))))

  (context "ws-inc-counter"
    (context "failure"
      (it "if occupant not found"
        (let [response (sut/ws-inc-counter {:connection-id "blah"})]
          (should= :fail (:status response))
          (should= "Occupant not found" (apic/flash-text response 0))))

      (it "if room not found"
        (let [response (sut/ws-inc-counter {:connection-id (:conn-id @tf2/spy)})]
          (should= :fail (:status response))
          (should= "Room not found" (apic/flash-text response 0))))

      (it "if game not found"
        (db/delete-all :game)
        (db/delete-all :game-room)
        (let [response (sut/ws-inc-counter {:connection-id (:conn-id @tf2/heavy)})]
          (should= :fail (:status response))
          (should= "Game not found" (apic/flash-text response 0)))))

    (context "success"
      (redefs-around [dispatch/push-to-connections! (stub :push-to-connections!)])

      (it "updates counter"
        (let [response (sut/ws-inc-counter {:connection-id (:conn-id @tf2/heavy)})]
          (should= :ok (:status response))
          (should= 1 (:counter (db/entity (:id @tf2/ctf))))))

      (it "disscout to occupants"
        (let [game @tf2/ctf
              response (sut/ws-inc-counter {:connection-id (:conn-id @tf2/heavy)})]
          (should= :ok (:status response))
          (should-have-invoked :push-to-connections! {:with [(map (comp :conn-id db/entity) (:occupants @tf2/sawmill))
                                                             :game/update
                                                             (update game :counter inc)]}))))))
