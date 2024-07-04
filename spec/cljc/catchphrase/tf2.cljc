(ns catchphrase.tf2
  (:require [catchphrase.schema.full :as schema]
            [c3kit.bucket.api :as db]
            [c3kit.bucket.spec-helperc :as helperc]
            [catchphrase.occupantc :as occuantc]
            [catchphrase.roomc :as roomc]
            [reagent.core :as reagent]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [before]])
  #?(:clj (:import (clojure.lang IDeref))))

(def sawmill-code "shrine")
(def egypt-code "depths")

(def sawmill-atom (atom nil))
(def heavy-atom (atom nil))
(def medic-atom (atom nil))
(def scout-atom (atom nil))
(def spy-atom (atom nil))
(def egypt-atom (atom nil))
(def koth-atom (atom nil))

(deftype Entity [atm]
  #?(:clj IDeref :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [this] (db/reload @atm)))

(def sawmill (Entity. sawmill-atom))                        ;; a populated room
(def heavy (Entity. heavy-atom))                            ;; a occupant at firelink
(def medic (Entity. medic-atom))                            ;; a occupant at firelink
(def scout (Entity. scout-atom))                            ;; a occupant at firelink
(def spy (Entity. spy-atom))                                ;; a occupant who hasn't joined
(def egypt (Entity. egypt-atom))                            ;; an empty room
(def koth (Entity. koth-atom))                              ;; a game for firelink

(defn init []
  (reset! sawmill-atom (roomc/create-room! sawmill-code))
  (reset! egypt-atom (roomc/create-room! egypt-code))
  (reset! heavy-atom (db/tx (occuantc/->occupant "Lautrec" "conn-lautrec")))
  (reset! medic-atom (db/tx (occuantc/->occupant "Kingseeker Frampt" "conn-frampt")))
  (reset! scout-atom (db/tx (occuantc/->occupant "Patches" "conn-patches")))
  (reset! spy-atom (db/tx (occuantc/->occupant "Laurentius" "conn-laurentius")))
  (reset! koth-atom (db/tx {:kind :game :room (:id @sawmill) :counter 0}))
  (roomc/add-occupant! @sawmill @heavy)
  (roomc/add-occupant! @sawmill @medic)
  (roomc/add-occupant! @sawmill @scout)
  (db/tx {:kind :game-room :game (:id @koth) :room (:id @sawmill)}))

(def memory-config {:impl :memory :store #?(:clj (atom nil) :cljs (reagent/atom nil))})

(defn with-schemas
  ([] (with-schemas schema/full-schema))
  ([& schemas] (helperc/with-schemas memory-config schemas)))

(defn init-with-schemas []
  (list (with-schemas)
        (before (init))))