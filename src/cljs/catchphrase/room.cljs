(ns catchphrase.room
  (:require [catchphrase.game :as game]
            [catchphrase.state :as state]
            [c3kit.apron.corec :as ccc]
            [c3kit.wire.js :as wjs]
            [c3kit.wire.websocket :as ws]
            [clojure.string :as str]
            [reagent.core :as reagent]
            [c3kit.bucket.api :as db]
            [catchphrase.occupant :as occupant]
            [catchphrase.page :as page]))

(defn- maybe-join-room! [nickname]
  (when (not (str/blank? nickname))
    (ws/call! :room/join
              {:nickname nickname :room-code @state/code}
              occupant/receive-join!)))

(defn nickname-prompt [_]
  (let [local-nickname-ratom (reagent/atom nil)]
    (fn [_]
      [:div.center-div.margin-top-plus-5
       {:id "-nickname-prompt"}
       [:h1 "Enter nickname to join room..."]
       [:div.center
        [:input
         {:type "text"
          :id "-nickname-input"
          :placeholder "Enter your nickname"
          :value @local-nickname-ratom
          :on-change #(reset! local-nickname-ratom (wjs/e-text %))}]
        [:button
         {:id "-join-button"
          :on-click #(maybe-join-room! @local-nickname-ratom)}
         "Join"]]])))

(defn- fetch-game []
  (ws/call! :game/fetch nil db/tx*))

(defn room-component []
  (reagent/create-class
    {:component-did-mount fetch-game
     :reagent-render
     (fn []
       (game/full))}))

(defn nickname-prompt-or-room [nickname-ratom]
  [:<>
   [:div {:id "-prompt-or-room"}
   (if (str/blank? @nickname-ratom)
     [nickname-prompt nickname-ratom]
     [room-component])]
   [:div {:style {:width "50vw"
                  :height "100vh"
                  :background-color "#a7584b"
                  :position "absolute"
                  :z-index "-1"
                  :left "50%"}}]
   [:div {:style {:width "50vw"
                  :height "100vh"
                  :background-color "#537d8b"
                  :position "absolute"
                  :z-index "-1"}}]])

(defn maybe-not-found []
  (if @state/room
    [nickname-prompt-or-room occupant/nickname]
    [:p#-not-found "Oops, we can't find your room..."]))

(defn- fetch-room []
  (ws/call! :room/fetch {:room-code @state/code} db/tx*))

(defn- clear-db! []
  (db/tx* (map db/soft-delete (db/find :room)))
  (db/tx* (map db/soft-delete (db/find :game))))

(defmethod page/entering! :room [_]
  (clear-db!)
  (maybe-join-room! @state/nickname)
  (fetch-room))

(defmethod page/exiting! :room [_]
  (reset! state/room-state {})
  (ws/call! :room/leave {} ccc/noop))

(defmethod page/render :room [_]
  [maybe-not-found])

(defmethod ws/push-handler :room/update [push]
  (db/tx* (:params push))
  (fetch-game))