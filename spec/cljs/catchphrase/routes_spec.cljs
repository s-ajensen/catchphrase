(ns catchphrase.routes-spec
  (:require-macros [catchphrase.spec-helperc :refer [it-routes]]
                   [speclj.core :refer [redefs-around around before context describe it should= stub with-stubs]])
  (:require [catchphrase.page :as page]
            [catchphrase.routes :as sut]
            [secretary.core :as secretary]
            [catchphrase.room :as room]
            [speclj.core]))

(describe "Routes"
  (with-stubs)
  (before (page/clear!)
          (secretary/reset-routes!)
          (sut/app-routes))

  (redefs-around [sut/load-page! (stub :load-page!)])

  (it-routes "/" :home)
  (it-routes "/room/sawmill" :room
             (should= "sawmill" @room/code))
  (it-routes "/room/egypt" :room
             (should= "egypt" @room/code)))