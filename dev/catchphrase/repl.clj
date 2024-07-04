(ns catchphrase.repl
  (:require
   [catchphrase.init :as init]
   [catchphrase.main :as main]))

(println "Welcome to the catchphrase REPL!")
(println "Initializing")
(init/install-legend!)
(main/start-db)
(require '[c3kit.bucket.api :as db])
