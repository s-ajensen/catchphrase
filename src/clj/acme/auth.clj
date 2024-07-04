(ns acme.auth
  (:require [c3kit.wire.ajax :as ajax]
            [c3kit.wire.jwt :as jwt]
            [c3kit.wire.websocket :as ws]))

(defn ajax-csrf-token [request]
  (let [{:keys [client-id]} (:jwt/payload request)]
    (-> {:ws-csrf-token      client-id
         :anti-forgery-token client-id}
      ajax/ok
      (jwt/copy-payload request))))

(defn websocket-open [request]
  (ws/handler request {:read-csrf jwt/client-id}))