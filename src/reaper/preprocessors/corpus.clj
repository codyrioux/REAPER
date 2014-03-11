(ns reaper.preprocessors.corpus
  "A number of utilities for preprocessing a corpus.
   Functions in this namespace provide transformations
   over a corpus, generally to be used before any processing begins.")

(defn- mapmap
  "Maps f over the elements of the seqs contained within coll."
  [f coll]
  (map #(map f %) coll))

(defn- mapfilter
  "Maps filter f over the seqs contained within coll."
  [f coll]
  (map (fn [x] (filter f x)) coll))

(defn remove-quotes
  "Removes any sentences in the corpus containing quotes."
  [corpus]
  (mapfilter #(not (or
                     (re-matches #".*``.*''.*" %)
                     (re-matches #".*\".*\".*" %)
                     (re-matches #".*''.*" %)
                     (re-matches #".*``.*" %)))
             corpus))

(defn remove-brackets
  "Removes any sentences in the corpus containing quotes."
  [corpus]
  (mapfilter #(not (or
                     (re-matches #".*\(.*" %)
                     (re-matches #".*\).*" %)))
             corpus))

(defn remove-short-sentences
  "Removes any sentences in the corpus shorter than length l."
  [l corpus]
  (mapfilter #(< l (count %)) corpus))
