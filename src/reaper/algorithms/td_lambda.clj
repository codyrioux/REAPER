(ns reaper.algorithms.td-lambda
  "An implementation of tablular backwards TD(lambda) from Sutton and Barto's
   Reinforcement Learning: An Introduction."
  (:require [reaper.util :as util]))

(defn policy
  "Applies the policy that would be defined by v to select an action.
   Randomly selects between equivalent actions.
   Assumes an initial value of 0.01 for each state, uses zero
   when set to greedy (param r = 0)"
  [fe v m r sp s]
  (let
    [actions (sp s)]
    (cond
      (empty? actions) nil
      (zero? r) (util/argmax #(get v (fe (m s %)) 0) actions)
      :else
      (let
        [boltzmann-sum (reduce + 0 (map #(java.lang.StrictMath/exp
                                           (/ (get v (fe (m s %)) 0) r)) actions))
         boltzmann-probs (zipmap actions (map #(/ (java.lang.StrictMath/exp
                                                    (/ (get v (fe (m s %)) 0) r)) boltzmann-sum) actions))]
        (util/rand-from-probdist boltzmann-probs)))))

(defn learn
  "Implements TD(lambda) for summarization using reinforcement learning.

   m: Generative model in which (m s a) => s'
   reward: Reward function (reward s) => double
   fe: feature extraction function for states.
   initial-state: The initial state for each episode
   sp: Source of actions for a state where (sp s) => [a1, a2, an]
   lambda: The trace decay parameter.
   alpha: The learning rate, a small positive number.
   y: Discount rate for future states.
   nmax: Number of episodes for learning.
   terminal?: A function that determines if a state s is a terminal state.

   Returns a policy function p in which (p s) => a."
  [m reward fe initial-state sp lambda y alpha nmax terminal? reset]
  (loop
    [n nmax
     v {}
     e {}
     tk (* 1.0 (Math/pow 0.987 -1))
     alphak (/ (* alpha 101) (+ 100 (Math/pow 0 1.1)))]
    (cond
      (= 0 n) (partial policy fe v m 0 sp)
      :else
      (let
        [[vnext enext]
         (loop
           [s initial-state
            v v
            e e ]
           (let [s' (m s (policy fe v m tk sp s))
                 r (reward s')
                 delta (- (+ r (* y (get v (fe s') 0))) (get v (fe s) 0))
                 e (assoc e (fe s) (inc (get e (fe s) 0)))
                 v (if (nil? (v (fe s))) (assoc v (fe s) 0) v)
                 v (util/fkmap #(+ %2 (* alphak delta (e %1 0))) v)
                 e (util/fmap #(* y lambda %)  e) ]
             (if (terminal? s') [v e] (recur s' v e))))]
        (recur (dec n) vnext (if reset {} enext)
               (* 1.0 (Math/pow 0.987 (- nmax (dec n) 1)))
               (/ (* alpha 101) (+ 100 (Math/pow (- nmax (dec n)) 1.1))))))))
