(ns catchphrase.game-spec
  (:require [catchphrase.teamc :as teamc]
            [catchphrase.tf2 :as tf2]
            [catchphrase.dispatch :as dispatch]
            [catchphrase.game :as sut]
            [c3kit.bucket.api :as db]
            [c3kit.wire.apic :as apic]
            [speclj.core :refer :all]))

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
              [red blu] (teamc/by-game @tf2/ctf)]
          (should= :ok (:status response))
          (should= [@tf2/ctf red blu] (:payload response))))))

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
