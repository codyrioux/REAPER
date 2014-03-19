(ns reaper.rewards.util
  "A set of utility functions for developing reward and scoring
   functions."
  (:require [reaper.util :as util]
            [reaper.algorithms.kmeans :as km]
            [incanter.stats :as stats]
            [clojure.set :refer [intersection]]
            [reaper.features.tfidf :refer [make-tfidf-vectorizer]]))

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
  [corpus wij csim vectorize n & {:keys
                                  [distance sim query beta niters]
                                  :or
                                  {distance stats/euclidean-distance
                                   sim stats/cosine-similarity
                                   query nil
                                   beta 0.5
                                   niters 10}}]
  (let
    [groups (filter seq
                    (map set (km/k-means (flatten corpus)
                                         distance
                                         vectorize
                                         (quot (count (flatten corpus)) n)
                                         niters)))
     qvectorizer (if query (make-tfidf-vectorizer [[query]] (count query) :remove-stopwords true :stem true) nil)
     qvec (if query (qvectorizer query) nil)
     e-to-q (if query (util/normalize-map-weights
                        (zipmap (flatten corpus)
                                (map #(try
                                        (sim qvec (qvectorizer %))
                                        (catch Exception e 0.0))
                                     (flatten corpus))))
              {})]
    (fn [s]
      (reduce + (map 
                  #(Math/sqrt
                     (reduce + (map (fn [sentence]
                                      (+ (* (/ beta (count (flatten corpus))) (csim sentence))
                                         (* (- 1 beta) (if query (get e-to-q sentence 0) 0))))
                                    (intersection % (set (first s))))))
                  groups)))))



(defn- average-score
  [unit-coll score unit]
  (reduce + (map #(score unit %)
                 (remove #{unit} unit-coll))))

(defn make-tfidf-sim
  ""
  [corpus]
  (let
    []
    ))

(defn make-coverage-fn
  "Given the corpus, and a pairwire scoring function score(x, y) produces
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
