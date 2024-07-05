(ns catchphrase.word-spec
  (:require [catchphrase.word :as sut]
            [speclj.core :refer :all]))

(describe "word"
  (redefs-around [shuffle #(concat (rest %) [(first %)])])

  ; why why why
  #_(context "ws-next-word"
    (it "sends first word"
      (let [response (sut/ws-next-word nil)]
        (should= :ok (:status response))
        (should= "standing ovation" (:payload response))))

    (it "sends next word"
      (let [response (sut/ws-next-word nil)]
        (should= :ok (:status response))
        (should= "alarm clock" (:payload response))))))