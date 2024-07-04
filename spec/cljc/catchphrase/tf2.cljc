(ns catchphrase.tf2
  (:require [catchphrase.schema.full :as schema]
            [c3kit.bucket.api :as db]
            [c3kit.bucket.spec-helperc :as helperc]
            [catchphrase.occupantc :as occuantc]
            [catchphrase.roomc :as roomc]
            [reagent.core :as reagent]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [before]])
  #?(:clj (:import (clojure.lang IDeref))))

(def firelink-code "shrine")
(def depths-code "depths")

(def firelink-atom (atom nil))
(def lautrec-atom (atom nil))
(def frampt-atom (atom nil))
(def patches-atom (atom nil))
(def laurentius-atom (atom nil))
(def depths-atom (atom nil))
(def dark-souls-atom (atom nil))

(deftype Entity [atm]
  #?(:clj IDeref :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [this] (db/reload @atm)))

(def firelink (Entity. firelink-atom))                      ;; a populated room
(def lautrec (Entity. lautrec-atom))                        ;; a occupant at firelink
(def frampt (Entity. frampt-atom))                          ;; a occupant at firelink
(def patches (Entity. patches-atom))                        ;; a occupant at firelink
(def laurentius (Entity. laurentius-atom))                  ;; a occupant who hasn't joined
(def depths (Entity. depths-atom))                          ;; an empty room
(def dark-souls (Entity. dark-souls-atom))                  ;; a game for firelink

(defn init []
  (reset! firelink-atom (roomc/create-room! firelink-code))
  (reset! depths-atom (roomc/create-room! depths-code))
  (reset! lautrec-atom (db/tx (occuantc/->occupant "Lautrec" "conn-lautrec")))
  (reset! frampt-atom (db/tx (occuantc/->occupant "Kingseeker Frampt" "conn-frampt")))
  (reset! patches-atom (db/tx (occuantc/->occupant "Patches" "conn-patches")))
  (reset! laurentius-atom (db/tx (occuantc/->occupant "Laurentius" "conn-laurentius")))
  (reset! dark-souls-atom (db/tx {:kind :game :room (:id @firelink) :counter 0}))
  (roomc/add-occupant! @firelink @lautrec)
  (roomc/add-occupant! @firelink @frampt)
  (roomc/add-occupant! @firelink @patches)
  (db/tx {:kind :game-room :game (:id @dark-souls) :room (:id @firelink)}))

(def memory-config {:impl :memory :store #?(:clj (atom nil) :cljs (reagent/atom nil))})

(defn with-schemas
  ([] (with-schemas schema/full-schema))
  ([& schemas] (helperc/with-schemas memory-config schemas)))

(defn init-with-schemas []
  (list (with-schemas)
    (before (init))))