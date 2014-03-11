(ns reaper.algorithms.sarsa
  "An implementation of Sarsa(lambda) from Sutton and Barto's
   Reinforcement Learning: An Introduction."
  (:require [reaper.util :as util]))

(defn policy
  "Applies the policy that would be defined by v to select an action.
   Randomly selects between equivalent actions.
   Assumes an initial value of 0.01 for each state, uses zero
   when set to greedy (param r = 0)"
  [fe q r sp s]
  (let
    [actions (sp s)]
    (cond
      (empty? actions) nil
      (= 0 r) (util/argmax #(get q [(fe s) %] 0) actions)
      :else
      (let
        [boltzmann-sum (reduce + 0 (map #(java.lang.StrictMath/exp
                                           (/ (get q [(fe s) %] 0) r)) actions))
         boltzmann-probs (zipmap actions (map #(/ (java.lang.StrictMath/exp
                                                    (/ (get q [(fe s) %] 0) r)) boltzmann-sum) actions))]
        (util/rand-from-probdist boltzmann-probs)))))

(defn learn
  "Implements Sarsa(lambda) for summarization using reinforcement learning.

   m: Generative model in which (m s a) => s'
   reward: Reward function (reward s) => double
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
     q {}
     e {}
     tk (* 1.0 (Math/pow 0.987 -1))
     alphak (/ (* alpha 101) (+ 100 (Math/pow 0 1.1)))]
    (cond
      (= 0 n) (partial policy fe q 0 sp)
      :else
      (let
        [[qnext enext]
         (loop
           [s initial-state
            a (policy fe q tk sp s)
            q q
            e e]
           (let [s' (m s a)
                 a (last (second s'))
                 a' (policy fe q tk sp s') 
                 delta (- (+ (reward s') (* y (get q [(fe s') a'] 0))) (get q [(fe s) a] 0))
                 e (assoc e [(fe s)] (inc (get e [(fe s) a] 0)))
                 q (if (= nil (q [(fe s) a])) (assoc q [(fe s) a] 0) q)
                 q (util/fkmap #(+ %2 (* alphak delta (get e %1 0))) q)
                 e (util/fmap #(* y lambda %) e) ]
             (if (terminal? s') [q e] (recur s' a' q e))))]
        (recur (dec n) qnext (if reset {} enext)
               (* 1.0 (Math/pow 0.987 (- nmax (dec n) 1)))
               (/ (* alpha 101) (+ 100 (Math/pow (- nmax (dec n)) 1.1))))))))
