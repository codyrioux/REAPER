(ns reaper.tools.stats
  (:require [reaper.util :as util]
            [incanter.core :as i]
            [incanter.stats :as stats]))

(defn kl-divergence
  "Calculates Kullback Libeler Divergence of vectors x and y."
  [x y]
  (i/sum
    (i/mult x
            (i/log
              (i/div x y)))))

(defn js-divergence
  "Calculates the Jensen Shannon Divergence of vectors b and c."
  [b c]
  (let [m (i/div (i/plus b c) 2)]
    (* (/ 1 2)
       (kullback-leibler-divergence b m)
       (kullback-leibler-divergence c m))))

(defn kl-sim
  "A relevance measure between two vectors."
  [b c]
  (- 1 (kullback-leibler-divergence b c)))

(defn js-sim
  "A relevance measure between two vectors."
  [b c]
  (- 1 (jensen-shannon-divergence b c)))
