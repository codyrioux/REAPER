(ns reaper.experiments.simple
  (:require [reaper.experiments.core :refer :all]
            [reaper.algorithms.greedy :refer :all]
            [reaper.rewards.submodular.rouge :refer [make-rouge-n]])
  (:gen-class))

(defn -main
  [length-lim]
  (let
    [corpus [(line-seq (java.io.BufferedReader. *in*))]
     actions (map #(vec [:insert %]) (flatten corpus))
     sp (partial sp actions (read-string length-lim))
     reward (make-rouge-n corpus 2)]
    (println (clojure.string/join "\n"
                                  (first
                                    (greedy-knapsack
                                      [[] [] 0]
                                      terminal?
                                      cost
                                      m
                                      sp
                                      reward
                                      (read-string length-lim)))))))
