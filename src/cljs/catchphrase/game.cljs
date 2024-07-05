(ns catchphrase.game
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.websocket :as ws]
            [catchphrase.occupant :as occupant]
            [catchphrase.state :as state]
            [reagent.core :as reagent]))

(def active-occupant (reagent/track #(db/entity (:active-occupant @state/game))))

(def blu (reagent/track #(db/ffind-by :team :game (:id @state/game) :color :blu)))
(defn on-blu? [occupant] (= (:id @blu) (:team occupant)))
(def blu-occupants (reagent/track #(filterv on-blu? @state/occupants)))

(def red (reagent/track #(db/ffind-by :team :game (:id @state/game) :color :red)))
(defn on-red? [occupant] (= (:id @red) (:team occupant)))
(def red-occupants (reagent/track #(filterv on-red? @state/occupants)))

(def current-word (reagent/atom nil))

(defn receive-new-word [word]
  (reset! current-word word))

; sorry
(ws/call! :word/next-word nil receive-new-word)

(defn display-team [prefix occupants]
  [:ul (ccc/for-all [occupant occupants]
         [:li
          {:key (:id occupant)
           :id  (str "-" prefix "-" (:id occupant))}
          (str (:nickname occupant) (when (state/host? occupant) " (Host)"))])])

(defmethod ws/push-handler :game/update [push]
  (db/tx* (:params push))
  (if (= :round-end (:state @state/game))
    (ws/call! :word/next-word nil receive-new-word)))

(defmulti state-label :state)

(defmethod state-label :default [_]
  "Waiting for host to start the round...")

(defmethod state-label :started [_]
  (str "It's " (:nickname @active-occupant) "'s turn! Time is ticking!"))

(defmethod state-label :round-end [_]
  "Round ended...")

(defmethod state-label :over [_]
  "Game Over!")

(defn full []
  [:div.main-container
   {:id "-room"
    :style {:color "#f4f3de"}}
   [:div.left-container
    [:br]
    [:br]
    [:h3 "Team Blu"]
    [:p#-blu-pts (str "POINTS: " (:points @blu))]
    (display-team "blu" @blu-occupants)]
   [:div.center
    [:div.game-container
     [:h1 "catchphrase"]
     [:div#-game-container.game
      [:p#-game-state.text-align-center (state-label @state/game)]
      (when (and (= (:id @active-occupant) (:id @occupant/current))
                 (not= :over (:state @state/game)))
        [:<>
         [:p "Current Phrase:"]
         [:h1#-phrase @current-word]])
      (when (and (state/host?) (or (= :lobby (:state @state/game))
                                   (= :round-end (:state @state/game))))
        [:button#-start-button
         {:on-click #(do (ws/call! :game/start nil db/tx*)
                         (ws/call! :word/next-word nil receive-new-word))}
         "Start Round"])
      (when (and (= (:id @active-occupant) (:id @occupant/current))
                 (= :started (:state @state/game)))
        [:<>
         [:button#-new-word-button
          {:on-click #(ws/call! :word/next-word nil receive-new-word)}
          "New Phrase"]
         [:button#-advance-button
          {:on-click #(do (ws/call! :game/advance nil ccc/noop)
                          (ws/call! :word/next-word nil receive-new-word))}
          "Advance Game"]])
      (when (and (= (:id @active-occupant) (:id @occupant/current))
                 (= :round-end (:state @state/game)))
        [:<>
         [:button#-blu-pts-button
          {:on-click #(ws/call! :game/inc-points {:team (:id @blu)} ccc/noop)}
          "Give Blu Points"]
         [:button#-red-pts-button
          {:on-click #(ws/call! :game/inc-points {:team (:id @red)} ccc/noop)}
          "Give Red Points"]])]]]
   [:div.right-container
    [:br]
    [:br]
    [:h3 "Team Red"]
    [:p#-red-pts (str "POINTS: " (:points @red))]
    (display-team "red" @red-occupants)]])