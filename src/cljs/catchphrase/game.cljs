(ns catchphrase.game
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.websocket :as ws]))

(defmethod ws/push-handler :game/update [push]
  (db/tx* (:params push)))

(defn full [game]
  [:div#-game-container.game
   [:p#-counter.text-align-center (:counter @game)]
   [:button#-inc-btn
    {:on-click #(ws/call! :game/inc-counter [] ccc/noop)}
    "Click me!"]])