(ns catchphrase.tf2
  (:require [catchphrase.schema.full :as schema]
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
(def egypt-atom (atom nil))
(def ctf-atom (atom nil))

(deftype Entity [atm]
  #?(:clj IDeref :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [this] (db/reload @atm)))

(def sawmill (Entity. sawmill-atom))                        ;; a populated room
(def heavy (Entity. heavy-atom))                            ;; a occupant at sawmill
(def medic (Entity. medic-atom))                            ;; a occupant at sawmill
(def scout (Entity. scout-atom))                            ;; a occupant at sawmill
(def spy (Entity. spy-atom))                                ;; a occupant who hasn't joined
(def egypt (Entity. egypt-atom))                            ;; an empty room
(def ctf (Entity. ctf-atom))                                ;; a game for sawmill

(defn init []
  (reset! sawmill-atom (roomc/create-room! sawmill-code))
  (reset! egypt-atom (roomc/create-room! egypt-code))
  (reset! heavy-atom (db/tx (occuantc/->occupant "heavy" "conn-heavy")))
  (reset! medic-atom (db/tx (occuantc/->occupant "medic" "conn-medic")))
  (reset! scout-atom (db/tx (occuantc/->occupant "scout" "conn-scout")))
  (reset! spy-atom (db/tx (occuantc/->occupant "spy" "conn-spy")))
  (reset! ctf-atom (db/tx {:kind :game :room (:id @sawmill) :counter 0}))
  (roomc/add-occupant! @sawmill @heavy)
  (roomc/add-occupant! @sawmill @medic)
  (roomc/add-occupant! @sawmill @scout)
  (db/tx {:kind :game-room :game (:id @ctf) :room (:id @sawmill)}))

(def memory-config {:impl :memory :store #?(:clj (atom nil) :cljs (reagent/atom nil))})

(defn with-schemas
  ([] (with-schemas schema/full-schema))
  ([& schemas] (helperc/with-schemas memory-config schemas)))

(defn init-with-schemas []
  (list (with-schemas)
        (before (init))))