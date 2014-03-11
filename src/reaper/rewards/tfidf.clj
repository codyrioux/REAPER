(ns reaper.rewards.tfidf
  (:require
    [reaper.util :as util]
    [reaper.tools.vectorization :refer [make-tfidf-vectorizer]]
    [incanter.stats :refer [cosine-similarity]]
    [clojure.core.memoize :as memo]
    [clojure.core.reducers :as r]))

(defn make-tfidf-sim
  "Creates a pairwise similarity function taking two sentences s1 and s2,
   returns the cosine similairty."
  [corpus n & {:keys [remove-stopwords stem]
               :or {remove-stopwords false stem false}}]
  (let
    [vectorize (make-tfidf-vectorizer corpus n
                                      :remove-stopwords remove-stopwords :stem stem)]
    (fn [s1 s2]
      (try
        (cosine-similarity (vectorize s1) (vectorize s2))
        (catch Exception e 0.0)))))

(defn make-tfidf-corpus-sim
  [corpus n & {:keys [remove-stopwords stem]
               :or {remove-stopwords false stem false}}]
  (let
    [vectorize (make-tfidf-vectorizer corpus n
                                      :remove-stopwords remove-stopwords :stem stem)
     cached-sims (zipmap (flatten corpus)
                         (map #(reduce + (map (fn [x] (try
                                                        (cosine-similarity (vectorize %) (vectorize x))
                                                        (catch Exception e 0.0)))
                                              (flatten corpus)))
                              (flatten corpus)))]
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
