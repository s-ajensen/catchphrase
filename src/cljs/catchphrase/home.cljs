(ns catchphrase.home
  (:require [accountant.core :as accountant]
            [c3kit.wire.js :as wjs]
            [c3kit.wire.websocket :as ws]
            [clojure.string :as str]
            [catchphrase.state :as state]
            [catchphrase.page :as page]))

(defn navigate-to-room! [[code]]
  (accountant/navigate! (str "/room/" code)))

(defn- create-room! [nickname]
  (when (not (str/blank? nickname))
    (ws/call! :room/create {:nickname nickname} navigate-to-room!)))

(defn home [nickname-ratom]
  [:div [:div {:style {:width "50%"
                      :height "100%"
                      :background-color "#a7584b"
                      :position "absolute"
                      :z-index "-1"
                      :left "50%"
                      :overflow "hidden"}}]
   [:div {:style {:width "50%"
                  :height "100%"
                  :background-color "#537d8b"
                  :position "absolute"
                  :z-index "-1"
                  :overflow "hidden"}}]
   [:div.homepage-container
        {:style {:color "#f4f3de"}}
   [:h1 "Welcome to catchphrase"]
   [:div.nickname-input
    [:input
     {:type        "text"
      :id          "-nickname-input"
      :placeholder "Enter your nickname"
      :value       @nickname-ratom
      :on-change   #(reset! nickname-ratom (wjs/e-text %))}]]
   [:div.room-actions
    [:button
     {:id       "-create-room-button"
      :on-click #(create-room! @nickname-ratom)}
     "Create Room"]]]])

(defmethod page/render :home [_]
  [home state/nickname])