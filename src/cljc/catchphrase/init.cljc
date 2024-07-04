(ns catchphrase.init
  (:require #?(:cljs [catchphrase.core :as core])
            #?(:cljs [catchphrase.page :as page])
            #?(:cljs [reagent.core :as reagent])
            [catchphrase.schema.game-room :as game-room]
            [c3kit.apron.legend :as legend]
            [c3kit.bucket.api :as db]
            [c3kit.bucket.memory]
            [c3kit.wire.api :as api]
            [catchphrase.config :as config]
            [catchphrase.schema.full :as schema]
            [catchphrase.schema.game :as game]
            [catchphrase.schema.occupant :as occupant]
            [catchphrase.schema.room :as room]
            [catchphrase.schema.team :as team]))

(defn install-legend! []
  (legend/init! {:room       room/room
                 :occupant   occupant/occupant
                 :game       game/game
                 :game-room  game-room/game-room
                 :team       team/team
                 :db/retract legend/retract
                 }))

#?(:cljs (defn install-reagent-db-atom! []
           (db/set-impl! (db/create-db config/bucket schema/full-schema))))

(defn configure-api! []
  (api/configure! #?(:clj  {:ws-handlers 'catchphrase.routes/ws-handlers
                            :version     (api/version-from-js-file (if config/development? "public/cljs/catchphrase_dev.js" "public/cljs/catchphrase.js"))}
                     :cljs {:redirect-fn       core/goto!
                            })))
