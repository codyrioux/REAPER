(ns reaper.rewards.submodular.rouge
  "A set of utility functions for implementing ngram recall rewards
   on summaries.
   The functions in this namespace are monotone nondecreasing
   submodular. "
  (:require [reaper.tools.ngrams :as ngrams]))

(defn- count-match
  [s-ngrams r-ngrams]
  (reduce + (map #(max (get r-ngrams % 0) (get s-ngrams % 0)) (keys s-ngrams))))

(defn rouge-n
  [s-ngrams reference]
  (cond
    (empty? reference) 0
    :else
    (/ (count-match s-ngrams reference)
       (reduce + (vals reference)))))

(defn rouge-n-multi
  [s-ngrams r-ngrams-coll]
  (apply max (conj (map #(rouge-n s-ngrams %) r-ngrams-coll) 0)))

(defn make-rouge-n
  "Creates a rouge-n function with the reference summaries
   set to a preprocessesd model based on the corpus."
  [corpus n]
  (let
    [ngram-model (ngrams/corpus->ngrams corpus n)]
    (fn [s] (rouge-n 
                (ngrams/document->ngrams (first s) n)
                ngram-model))))
