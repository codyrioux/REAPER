(ns reaper.experiments.core
  "A couple of core reusable functions useful for these experiments."
  (:require [reaper.util :as util]))

(def cost-ignored-tokens #{"," "'" "." "(" ")"})
(defn cost [[action content]] (count (remove cost-ignored-tokens
                                             (util/tokenizer content))))
(defn length [s] (reduce + (map #(cost [:_ %]) (first s))))
(defn terminal? [s] (= 1 (nth s 2)))

(defn m
  "Takes a state and action and returns a new state and reward.
   s: State. In the form of a summary as in eacl2014.features (S, A, f, c)
   a: Action [:insert sentence-content]

   Return: A transitioned state."
  [s a]
  (cond
    (= (nth s 2) 1) s
    (nil? a) s
    (= :insert (first a))
    [(conj (first s) (second a)) (conj (second s) a) 0 0]
    (= :finish (first a))
    [(first s) (conj (second s) a) 1 0]))

(defn sp
  "Observes a state and determines a set of possible actions.
   Right now that just means allowing insertion of any sentence not currently
   already in the summary.

   actions: A list of possible actions.
   s: The current state as described in eacl2014.features (S, A, f)

   Returns: A list of potential actions in the form [:action-type string-describing-it]"
  [actions k s]
  (cond
    (< k (length s)) []
    (= (nth s 2) 0) (remove (set (second s)) actions)
    (= (nth s 2) 1) []))

(defn postprocess
  "A post-processing function for a summary that removes
   certain undesirable side effects of any preprocessing."
  [sentences]
  (map #(->
          %
          (clojure.string/replace #" , " ", ")
          (clojure.string/replace #" \?" "?")
          (clojure.string/replace #" 's" "'s")
          (clojure.string/replace #" n't" "n't")
          (clojure.string/replace #" \. " ". "))
       sentences))
