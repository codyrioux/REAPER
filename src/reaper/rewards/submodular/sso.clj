(ns reaper.rewards.submodular.sso
  (:require
    [reaper.util :as util]))

(defn- get-sso-score
  [sso-model s x]
  (/ (reduce + (map
              #(if (<= (get sso-model % 0) (get sso-model x 0)) 1 0)
              (util/all-before (first s) x)))
     (count (util/all-before (first s) x))))

(defn make-sso-fn
  [corpus]
  (let
    [sso-model (apply merge (map (fn [doc] (zipmap doc (range (count doc)) )) corpus))]
    (fn [s]
      (cond
        (= 0 (count (first s))) 0
        (= 1 (count (first s))) 1
        :else (/
               (reduce +
                       (map #(get-sso-score sso-model s %) (rest (first s))))
               (count (rest (first s))))))))
