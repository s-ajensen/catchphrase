(ns catchphrase.page
  (:require [c3kit.apron.log :as log]
            [reagent.core :as reagent]))

(def state (reagent/atom {}))

(def clear! #(reset! state {}))

(defn push-page [state page]
  (-> state
    (assoc :previous-page (:page state))
    (assoc :page page)))

(defn install-page! [page] (swap! state push-page page))
(def current (reagent/track #(:page @state)))
(def previous (reagent/track #(:previous-page @state)))

(defn cursor
  ([path] (cursor path nil))
  ([path value]
   (let [c (reagent/cursor state path)]
     (reset! c value)
     c)))

(defmulti render identity)
(defmulti entering! identity)
(defmulti exiting! identity)
(defmulti reentering! identity) ;; when transitioning to the same page
(defmulti title identity)

(defmethod render :default [_]
  (log/error "DEFAULT render-page!")
  [:h1 "DEFAULT render-page"])

(defmethod entering! :default [_])
(defmethod exiting! :default [_])
(defmethod reentering! :default [_page])
(defmethod title :default [_page] "catchphrase")

(defn transition [page]
  (let [current-page (:page @state)]
    (if (= current-page page)
      (do
        (log/debug "reentering page:" page)
        (reentering! page))
      (do
        (log/debug "exiting page:" current-page)
        (exiting! current-page)
        (log/debug "entering page:" page)
        (entering! page)))))