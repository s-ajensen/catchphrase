(ns catchphrase.room-spec
  (:require [c3kit.bucket.api :as db]
            [c3kit.wire.apic :as apic]
            [catchphrase.tf2 :as tf2 :refer [sawmill egypt heavy medic scout]]
            [catchphrase.dispatch :as dispatch]
            [catchphrase.occupantc :as occupantc]
            [catchphrase.room :as sut]
            [catchphrase.roomc :as roomc]
            [speclj.core :refer :all]))

(def idx (atom 5))

(describe "Room"
  (with-stubs)
  (tf2/init-with-schemas)
  (before (reset! idx 5))
  (redefs-around [rand-nth (stub :rand {:invoke (fn [coll]
                                                  (swap! idx inc)
                                                  (nth coll @idx))})])

  (context "room id"
    (it "random 6 numbers/letters"
      (should= "89ABCD" (sut/new-code))))

  (context "ws-create-room"
    (it "success"
      (let [response (sut/ws-create-room {})
            room (roomc/by-code "89ABCD")
            game-room (db/ffind-by :game-room :room (:id room))]
        (should= :ok (:status response))
        (should= ["89ABCD"] (:payload response))
        (should= 0 (:counter (db/entity (:game game-room))))))

    (it "does not duplicate room codes"
      (db/tx (roomc/->room "89ABCD"))
      (sut/ws-create-room {})
      (should-not-be-nil (roomc/by-code "EFHJKL"))))

  (context "ws-join-room"
    (redefs-around [dispatch/push-to-occupants! (stub :push-to-occupants!)])

    (before (roomc/create-room! "asylum"))

    (it "missing room"
      (let [response (sut/ws-join-room {:params {:nickname "Solaire"}})]
        (should= :fail (:status response))
        (should-be-nil (:payload response))
        (should= "Missing room!" (apic/flash-text response 0))))

    (it "missing nickname"
      (let [response (sut/ws-join-room {:params {:room-code "asylum"}})]
        (should= :fail (:status response))
        (should-be-nil (:payload response))
        (should= "Missing nickname!" (apic/flash-text response 0))))

    (it "room does not exist"
      (let [response (sut/ws-join-room {:params {:nickname "Solaire" :room-code "parish"}})]
        (should= :fail (:status response))
        (should-be-nil (:payload response))
        (should= "Room does not exist!" (apic/flash-text response 0))
        (should-be-nil (occupantc/by-nickname "Solaire"))))

    (it "joins room"
      (let [response (sut/ws-join-room {:params        {:nickname "Sewer Rat" :room-code tf2/egypt-code}
                                        :connection-id "conn-rat"})]
        (should= :ok (:status response))
        (let [occupant (occupantc/by-nickname "Sewer Rat")]
          (should-not-be-nil occupant)
          (should= [@egypt occupant] (:payload response))
          (should= "conn-rat" (:conn-id occupant)))))

    (it "notifies occupants of new room state"
      (let [response (sut/ws-join-room {:params        {:nickname "Giant Crow" :room-code tf2/sawmill-code}
                                        :connection-id "conn-crow"})
            crow (occupantc/by-nickname "Giant Crow")]
        (should= :ok (:status response))
        (should-have-invoked :push-to-occupants! {:with [(map db/entity (:occupants @sawmill))
                                                         :room/update
                                                         [@sawmill crow]]})))

    (it "responds with current room state & all current occupants"
      (let [response (sut/ws-join-room {:params        {:nickname "Giant Crow" :room-code tf2/sawmill-code}
                                        :connection-id "conn-crow"})
            crow (occupantc/by-nickname "Giant Crow")]
        (should= :ok (:status response))
        (should= (set [@sawmill crow @heavy @medic @scout]) (set (:payload response))))))

  (context "ws-leave-room"
    (redefs-around [dispatch/push-to-occupants! (stub :push-to-occupants!)])

    (it "removes occupant from room"
      (sut/ws-leave-room {:connection-id "conn-scout"})
      (should-not-contain (:id @scout) (:occupants @sawmill))
      (should= (mapv :id [@heavy @medic]) (:occupants @sawmill)))

    (it "removes occupant from db"
      (sut/ws-leave-room {:connection-id "conn-scout"})
      (should-be-nil (occupantc/by-conn-id "conn-scout")))

    (it "notifies occupants of new room state"
      (sut/ws-leave-room {:connection-id "conn-scout"})
      (should-have-invoked :push-to-occupants! {:with [(map db/entity (:occupants @sawmill))
                                                       :room/update
                                                       [@sawmill]]}))

    (it "deletes room if last person leaves"
      (sut/ws-leave-room {:connection-id "conn-scout"})
      (sut/ws-leave-room {:connection-id "conn-medic"})
      (sut/ws-leave-room {:connection-id "conn-heavy"})
      (should-be-nil @tf2/sawmill)))

  (context "ws-fetch-room"
    (before (roomc/create-room! "egypt"))

    (it "missing room"
      (let [response (sut/ws-fetch-room {:params {}})]
        (should= :fail (:status response))
        (should-be-nil (:payload response))
        (should= "Missing room!" (apic/flash-text response 0))))

    (it "room does not exist"
      (let [response (sut/ws-fetch-room {:params {:room-code "parish"}})]
        (should= :fail (:status response))
        (should-be-nil (:payload response))
        (should= "Room does not exist!" (apic/flash-text response 0))))

    (it "fetches room"
      (let [[_ crow] (:payload (sut/ws-join-room {:params {:nickname "Giant Crow" :room-code tf2/egypt-code}}))
            response (sut/ws-fetch-room {:params {:room-code tf2/egypt-code}})]
        (should= :ok (:status response))
        (should= [@tf2/egypt crow] (:payload response))))))