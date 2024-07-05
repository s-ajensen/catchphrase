(ns catchphrase.routes
  (:require [catchphrase.config :as config]
            [c3kit.apron.util :as util]
            [c3kit.wire.ajax :as ajax]
            [clojure.string :as str]
            [compojure.core :as compojure :refer [defroutes routes]]
            [ring.util.response :as response]))

(defn wrap-prefix [handler prefix not-found-handler]
  (fn [request]
    (let [path (or (:path-info request) (:uri request))]
      (when (str/starts-with? path prefix)
        (let [request (assoc request :path-info (subs path (count prefix)))]
          (if-let [response (handler request)]
            response
            (not-found-handler request)))))))

(def resolve-handler
  (if config/development?
    (fn [handler-sym] (util/resolve-var handler-sym))
    (memoize (fn [handler-sym] (util/resolve-var handler-sym)))))

(defn lazy-handle
  "Reduces load burden of this ns, which is useful in development.
  Runtime errors will occur for missing handlers, but all the routes should be tested in routes_spec.
  Assumes all handlers take one parameter, request."
  [handler-sym request]
  (let [handler (resolve-handler handler-sym)]
    (handler request)))

(defmacro lazy-routes
  "Creates compojure route for each entry where the handler is lazily loaded.
  Why are params a hash-map instead of & args? -> Intellij nicely formats hash-maps as tables :-)"
  [table]
  `(routes
     ~@(for [[[path method] handler-sym] table]
         (let [method (if (= :any method) nil method)]
           (compojure/compile-route method path 'req `((lazy-handle '~handler-sym ~'req)))))))

(defn redirect-handler [path]
  (let [segments (str/split path #"/")
        segments (map #(if (str/starts-with? % ":") (keyword (subs % 1)) %) segments)]
    (fn [request]
      (let [params   (:params request)
            segments (map #(if (keyword? %) (get params %) %) segments)
            dest     (str/join "/" segments)]
        (response/redirect dest)))))

(defmacro redirect-routes [table]
  `(routes
     ~@(for [[[path method] dest] table]
         (let [method (if (= :any method) nil method)]
           (compojure/compile-route method path 'req `((redirect-handler ~dest)))))))

(def ws-handlers
  {
   :ws/close            'catchphrase.room/ws-leave-room
   :room/create         'catchphrase.room/ws-create-room
   :room/join           'catchphrase.room/ws-join-room
   :room/leave          'catchphrase.room/ws-leave-room
   :room/fetch          'catchphrase.room/ws-fetch-room
   :game/fetch          'catchphrase.game/ws-fetch-game
   :game/start          'catchphrase.game/ws-start-game
   :game/advance        'catchphrase.game/ws-advance-game
   :game/steal          'catchphrase.game/ws-steal-game
   :game/inc-counter    'catchphrase.game/ws-inc-counter
   })

(def ajax-routes-handler
  (-> (lazy-routes
        {
         ["/csrf-token" :get] catchphrase.auth/ajax-csrf-token
         })
    (wrap-prefix "/api" ajax/api-not-found-handler)
    ajax/wrap-ajax))

(def web-routes-handlers
  (lazy-routes
    {
     ["/" :get]               catchphrase.layouts/web-rich-client
     ["/room/:code" :get]     catchphrase.layouts/web-rich-client
     ["/user/websocket" :any] catchphrase.auth/websocket-open
     }))

(defroutes handler
           ajax-routes-handler
           web-routes-handlers
           )