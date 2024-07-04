(ns acme.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:require [accountant.core :as accountant]
            [acme.page :as page]
            [c3kit.apron.log :as log]
            [c3kit.wire.js :as wjs]
            [secretary.core :as secretary]
            [acme.room :as room]))

(defn dispatch! [uri]
  (log/debug "dispatching: " uri)
  (secretary/dispatch! uri))

(defn locate-route [route]
  (let [result (secretary/locate-route route)]
    (log/debug "locate-route: " route " -> " result)
    result))

(defn- hook-browser-navigation! []
  (accountant/configure-navigation! {:nav-handler dispatch! :path-exists? locate-route}))

(defn load-page! [page]
  (page/transition page)
  (wjs/scroll-to-top)
  (wjs/page-title= (page/title page))
  (page/install-page! page))

(defn app-routes []
  (secretary/set-config! :prefix "")

  (defroute "/" [] (load-page! :home))
  (defroute "/room/:code" [code]
    (room/install-room! code)
    (load-page! :room))

  (hook-browser-navigation!))