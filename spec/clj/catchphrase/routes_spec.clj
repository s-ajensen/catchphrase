(ns catchphrase.routes-spec
  (:require [catchphrase.routes :as routes]
            [catchphrase.spec-helper]
            [c3kit.wire.spec-helper :as wire-helper]
            [c3kit.wire.websocket :as ws]
            [catchphrase.layouts]
            [speclj.core :refer :all]
            [speclj.stub :as stub]))

(def args (atom :none))

;; MDM - Macros are used here to preserve line number when specs fail

(defmacro check-route [path method handler]
  `(let [stub-key# ~(keyword handler)]
     (require '~(symbol (namespace handler)))
     (with-redefs [~handler (stub stub-key#)]
       (routes/handler {:uri ~path :request-method ~method})
       (should-have-invoked stub-key#)
       (reset! args (stub/first-invocation-of stub-key#)))))

(defmacro test-route [path method handler & body]
  `(it ~path
     (check-route ~path ~method ~handler)
     ~@body))

(defmacro test-redirect [path method dest]
  `(it (str ~path " -> " ~dest)
     (let [response# (routes/handler {:uri ~path :request-method ~method})]
       (wire-helper/should-redirect-to response# ~dest))))

(defmacro test-webs [id sym]
  `(it (str "remote " ~id " -> " '~sym)
     (let [action# (ws/resolve-handler ~id)]
       (should-not= nil action#)
       (should= '~sym (.toSymbol action#)))))

(describe "Routes"
  (with-stubs)
  (before (reset! args :none))
  (redefs-around [c3kit.wire.api/version (constantly "fake-api-version")])

  ; Please keep these specs sorted alphabetically

  ;; web routes
  ; TODO - fixme
  (test-route "/" :get catchphrase.layouts/web-rich-client)
  (test-route "/room/sawmill" :get catchphrase.layouts/web-rich-client)

  ;; websocket handlers
  (test-webs :ws/close             catchphrase.room/ws-leave-room)
  (test-webs :room/create          catchphrase.room/ws-create-room)
  (test-webs :room/join            catchphrase.room/ws-join-room)
  (test-webs :room/leave           catchphrase.room/ws-leave-room)
  (test-webs :room/fetch           catchphrase.room/ws-fetch-room)
  (test-webs :game/fetch           catchphrase.game/ws-fetch-game)
  (test-webs :game/start           catchphrase.game/ws-start-game)
  (test-webs :game/advance         catchphrase.game/ws-advance-game)
  (test-webs :game/steal           catchphrase.game/ws-steal-game)
  (test-webs :game/inc-points      catchphrase.game/ws-inc-points)
  (test-webs :word/next-word       catchphrase.word/ws-next-word)

  (it "not-found global - nil - handled by http"
    (let [response (routes/handler {:uri "/blah" :request-method :get})]
      (should-be-nil response))))