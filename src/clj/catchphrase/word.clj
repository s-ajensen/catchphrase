(ns catchphrase.word
  (:require [c3kit.wire.apic :as apic]
            [clojure.string :as str]))

(def words (delay (cycle (shuffle (str/split-lines (slurp "words.txt"))))))
(def idx (atom 0))

(defn ws-next-word [_request]
  (let [word (nth @words @idx)]
    (swap! idx inc)
    (apic/ok word)))