(ns catchphrase.styles.main
  (:refer-clojure :exclude [rem])
  (:require [garden.def :as garden]
            [catchphrase.styles.core :as core]
            [catchphrase.styles.components.menus :as menus]
            [catchphrase.styles.elements.forms :as forms]
            [catchphrase.styles.elements.lists :as lists]
            [catchphrase.styles.elements.media :as media]
            [catchphrase.styles.elements.tables :as tables]
            [catchphrase.styles.elements.typography :as typography]
            [catchphrase.styles.layout.document :as document]
            [catchphrase.styles.layout.mini-classes :as mini-classes]
            [catchphrase.styles.layout.page :as page]
            [catchphrase.styles.layout.reset :as reset]
            [catchphrase.styles.layout.structure :as structure]
            [catchphrase.styles.media.responsive :as responsive]
            [catchphrase.styles.pages.authentication :as authentication]
            [catchphrase.styles.pages.authentication :as authentication]
            ))

(garden/defstyles screen

; Layout
;reset/screen
document/screen
page/screen
;structure/screen
mini-classes/screen

; Elements
;typography/screen
;forms/screen
;lists/screen
;media/screen
;tables/screen

; Componenents
menus/screen

; Pages
;authentication/screen

; Media
;responsive/screen

; Fonts
;core/fonts

)
