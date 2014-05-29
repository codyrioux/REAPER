(ns reaper.rewards.util
  "A set of utility functions for developing reward and scoring
   functions."
  (:require [reaper.util :as util]
            [kmeans-clj.core :as km]
            [incanter.stats :as stats]
            [clojure.set :refer :all]
            [clojure.core.async :refer [chan to-chan <!!]]
            [clojure.set :refer [intersection]]
            [reaper.features.tfidf :refer [make-tfidf-vectorizer]]))

(defn make-sim-fn
  "Given a vectorizer representation and a vector similarity function.
   Returns a pairwise similarity function between two vectors."
  [vectorize & {:keys [sim]
                 :or {sim stats/cosine-similarity}}]
  (fn [s1 s2]
    (try
      (sim (vectorize s1) (vectorize s2))
      (catch Exception e 0.0))))

(defn make-corpus-sim-fn
  "Calculates each elemenets similarity
   corpus: A corpus object representing the input set."
  [vectorize corpus & {:keys [sim]
                       :or {sim stats/cosine-similarity}}]
  (let
    [sim (make-sim-fn vectorize :sim sim)
     input (to-chan (flatten corpus))
     output (chan)
     results (util/sink output)
     _ (<!! (util/parallel
              (.availableProcessors (Runtime/getRuntime))
              (fn [x]
                (vec [x (reduce + (map (partial sim x)
                                       (flatten corpus)))]))
              input
              output))
     cached-sims (into {} @results)]
    (fn [x] (get cached-sims x 0.0))))

(defn make-recall-fn
  "Given a set of elements to recall, and an extractor function
   which can extract said elements from a summary produces a recall
   function which takes only a summary as an argument.
   Recall is by definition monotone submodular."
  [elements extractor]
  (partial 
    (fn [elements extr s]
      (/ (count (clojure.set/intersection (set elements) (set (extr s))))
         (count (set elements))))
    elements
    extractor)) 

(defn make-diversity-fn
  "Given a map of elements => weights (score), a vectorizer function which
   maps elements to vectors this function, and n the grouping factor. Will
   create |elements| / n groups.
   If supplied with optional query and beta keys the diversity fn will be
   query sensitive."
  [corpus csim vectorize n & {:keys [distance qsim
                                     beta niters
                                     vfactory]
                              :or
                              {distance stats/euclidean-distance
                               qsim nil
                               beta 0.5
                               niters 10}}]
  (let
    [groups (filter seq
                    (map set (km/k-means (flatten corpus)
                                         distance
                                         vectorize
                                         (quot (count (flatten corpus)) n)
                                         niters)))]
    (fn [s]
      (reduce + (map 
                  #(Math/sqrt
                     (reduce + (map (fn [sentence]
                                      (+ (* (/ beta (count (flatten corpus))) (csim sentence))
                                         (* (- 1 beta) (if qsim (qsim sentence) 0.0))))
                                    (intersection % (set (first s))))))
                  groups)))))

(defn- average-score
  [unit-coll score unit]
  (reduce + (map #(score unit %)
                 (remove #{unit} unit-coll))))

(defn make-coverage-fn
  "Given the corpus, and a pairwise scoring function score(x, y) produces
   a coverage scoring function. This precomputes corpus similarity for
   performance reasons."
  [corpus score alpha]
  (let [unit-to-score
        (zipmap
          (flatten corpus)
          (map #(average-score (flatten corpus) score %) (flatten corpus)))]
    (fn [alpha s]
      (reduce + (map #(min
                        (average-score (flatten (first s)) score %)
                        (* alpha (get unit-to-score % 0)))
                     (flatten corpus))))))
