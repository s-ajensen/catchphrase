(ns catchphrase.tf2
  (:require [catchphrase.game-roomc :as game-roomc]
            [catchphrase.gamec :as gamec]
            [catchphrase.schema.full :as schema]
            [c3kit.bucket.api :as db]
            [c3kit.bucket.spec-helperc :as helperc]
            [catchphrase.occupantc :as occuantc]
            [catchphrase.roomc :as roomc]
            [reagent.core :as reagent]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [before]])
  #?(:clj (:import (clojure.lang IDeref))))

(def sawmill-code "sawmill")
(def egypt-code "egypt")

(def sawmill-atom (atom nil))
(def heavy-atom (atom nil))
(def medic-atom (atom nil))
(def scout-atom (atom nil))
(def spy-atom (atom nil))
(def pyro-atom (atom nil))
(def demo-atom (atom nil))
(def egypt-atom (atom nil))
(def ctf-atom (atom nil))
(def cp-atom (atom nil))

(deftype Entity [atm]
  #?(:clj IDeref :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [this] (db/reload @atm)))

(def sawmill (Entity. sawmill-atom))                        ;; a populated room
(def heavy (Entity. heavy-atom))                            ;; a occupant at sawmill
(def medic (Entity. medic-atom))                            ;; a occupant at sawmill
(def scout (Entity. scout-atom))                            ;; a occupant at sawmill
(def spy (Entity. spy-atom))                                ;; a occupant who hasn't joined
(def pyro (Entity. pyro-atom))                              ;; a occupant who hasn't joined
(def demo (Entity. demo-atom))                              ;; a occupant who hasn't joined
(def egypt (Entity. egypt-atom))                            ;; an empty room
(def ctf (Entity. ctf-atom))                                ;; a game for sawmill
(def cp (Entity. cp-atom))                                  ;; a game for egypt

(defn init []
  (reset! sawmill-atom (roomc/create-room! sawmill-code))
  (reset! egypt-atom (roomc/create-room! egypt-code))
  (reset! heavy-atom (db/tx (occuantc/->occupant "heavy" "conn-heavy")))
  (reset! medic-atom (db/tx (occuantc/->occupant "medic" "conn-medic")))
  (reset! scout-atom (db/tx (occuantc/->occupant "scout" "conn-scout")))
  (reset! spy-atom (db/tx (occuantc/->occupant "spy" "conn-spy")))
  (reset! pyro-atom (db/tx (occuantc/->occupant "pyro" "conn-pyro")))
  (reset! demo-atom (db/tx (occuantc/->occupant "demo" "conn-demo")))
  (reset! ctf-atom (gamec/create-game!))
  (reset! cp-atom (gamec/create-game!))
  (game-roomc/create-game-room! @ctf @sawmill)
  (game-roomc/create-game-room! @cp @egypt)
  (roomc/join-room! @sawmill @heavy)
  (roomc/join-room! @sawmill @medic)
  (roomc/join-room! @sawmill @scout))

(def memory-config {:impl :memory :store #?(:clj (atom nil) :cljs (reagent/atom nil))})

(defn with-schemas
  ([] (with-schemas schema/full-schema))
  ([& schemas] (helperc/with-schemas memory-config schemas)))

(defn init-with-schemas []
  (list (with-schemas)
        (before (init))))