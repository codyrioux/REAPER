(ns reaper.algorithms.greedy
  (:require
    [reaper.util :as util]))

(defn greedy
  "A basic greedy algorithm that selects argmax(actions)
   at each time step until a terminal state is reached."
  [istate terminal? m sp reward]
  (loop
    [state istate]
    (cond
      (terminal? state) state
      :else (recur (m state (util/argmax #(reward (m state %)) (sp state)))))))

(defn greedy-knapsack
  "A greedy algorithm that selects argmax(actions)
   at each time step. Actions are subject to a knapsack
   constraint in which a fixed budget is used.
   Returns either the greedy summary or the best singleton
   element, depending on which has a higher reward"
  [istate terminal? cost m sp reward ibudget & {:keys [r]
                                                :or {r 0.0}}]
  (let [g (loop
            [state istate
             budget ibudget]
            (let
              [action (util/pargmax
                        #(/ (- (reward (m state %)) (reward state)) (Math/pow (cost % ) r))
                        (filter (fn [action] (>= budget (cost action))) (sp state)))]
              (cond
                (nil? action) state
                :else (recur (m state action)
                             (- budget (cost action))))))
        vstar (util/argmax #(reward (m istate %)) (filter #(< (cost %) ibudget) (sp istate)))]
    (if (> (reward vstar) (reward g) vsar g))))
