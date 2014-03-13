(ns reaper.rewards.tfidf
  (:require
    [reaper.util :refer :all]
    [reaper.tools.vectorization :refer [make-tfidf-vectorizer]]
    [clojure.core.async :refer [chan to-chan <!!]]
    [incanter.stats :refer [cosine-similarity]]
    [clojure.core.memoize :as memo]
    [clojure.core.reducers :as r]))

(defn make-tfidf-sim
  "Creates a pairwise similarity function taking two sentences s1 and s2,
   returns the cosine similairty."
  [corpus n & {:keys [remove-stopwords stem]
               :or {remove-stopwords false stem false}}]
  (let
    [vectorize (memo/fifo (make-tfidf-vectorizer corpus n
                                      :remove-stopwords remove-stopwords :stem stem)
                          :fifo/threshold (count (flatten corpus)))]
    (fn [s1 s2]
      (try
        (cosine-similarity (vectorize s1) (vectorize s2))
        (catch Exception e 0.0)))))

(defn make-tfidf-corpus-sim
  "Calculates each elemenets similarity"
  [corpus n & {:keys [remove-stopwords stem]
               :or {remove-stopwords false stem false}}]
  (let
    [vectorize (memo/fifo (make-tfidf-vectorizer corpus n
                                      :remove-stopwords remove-stopwords :stem stem) :fifo/threshold (count (flatten corpus)))
     input (to-chan (flatten corpus))
     output (chan)
     results (sink output)
     _ (<!! (parallel
              (.availableProcessors (Runtime/getRuntime))
              (fn [x]
                (vec [x (reduce + (map #(try
                                          (cosine-similarity (vectorize x) (vectorize %))
                                          (catch Exception e 0.0))
                               (flatten corpus)))]))
              input
              output))
     cached-sims (into {} @results)]
    (fn [x] (get cached-sims x 0.0))))


(defn make-l1
  "L1 Reward from Lin and Blimes 2011."
  [corpus sim csim n & {:keys [remove-stopwords stem]
                        :or {remove-stopwords false stem false}}]
  (let
    [alpha (/ 5 (count (flatten corpus)))]
    (fn
      [s]
      (r/fold + (r/map  
                  (fn [i] (min 
                            (reduce + (map #(sim % i) (first s)))
                            (* alpha (csim i))))
                  (flatten corpus))))))
