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

(def blu (reagent/track #(db/ffind-by :team :game (:id @state/game) :color :blu)))
(defn on-blu? [occupant] (= (:id @blu) (:team occupant)))
(def blu-occupants (reagent/track #(filterv on-blu? @state/occupants)))

(def red (reagent/track #(db/ffind-by :team :game (:id @state/game) :color :red)))
(defn on-red? [occupant] (= (:id @red) (:team occupant)))
(def red-occupants (reagent/track #(filterv on-red? @state/occupants)))

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

(defn display-team [prefix occupants]
  [:ul (ccc/for-all [occupant occupants]
    [:li
     {:key (:id occupant)
      :id  (str "-" prefix "-" (:id occupant))}
     (:nickname occupant)])])

(defn room-component []
  (reagent/create-class
    {:component-did-mount fetch-game
     :reagent-render
     (fn []
       [:div.main-container
        {:id "-room"}
        [:div.left-container
         [:br]
         [:br]
         [:h3 "Team Blu"]
         (display-team "blu" @blu-occupants)]
        [:div.center
         [:div.game-container
          [:h1 "catchphrase"]
          (game/full)]]
        [:div.right-container
         [:br]
         [:br]
         [:h3 "Team Red"]
         (display-team "red" @red-occupants)]])}))

(defn nickname-prompt-or-room [nickname-ratom]
  [:div {:id "-prompt-or-room"}
   (if (str/blank? @nickname-ratom)
     [nickname-prompt nickname-ratom]
     [room-component])])

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