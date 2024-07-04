(ns catchphrase.spec-helper
  (:require [catchphrase.init :as init]
            [c3kit.apron.log :as log]
            [speclj.core :refer :all]))

(log/warn!)
(init/install-legend!)
(init/configure-api!)

