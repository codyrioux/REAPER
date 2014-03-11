(ns reaper.tools.ngrams
    (:require [clojure.string :as str]
              [reaper.tools.stop-words :refer [corpus->stop-words]]
              [reaper.tools.porter-stemmer :refer [corpus->stemmed-corpus]]
              [reaper.util :as util]))

(defn ngrams [n coll]
    "Computes a map with the ngram as the key, and the frequency as the value."
    (frequencies  (partition n 1 coll)))

(defn ngrams-to-prob-dist  [coll]
    "Computes a probability distribution for the passed in ngram collection."
    (let  [x  (reduce +  (vals coll))]
          (map #(list  (first %)  (/  (second %) x)) coll)))

(defn get-ngrams-matching  [target ngrams]
    "Retuns a list of ngrams that begin with the list denoted by target.
    If target is empty then it will return all ngrams, as they all begin
       with empty."
    (if  (zero? (count target)) ngrams 
        (filter #(=  (take  (count target)  (first %)) target) ngrams)))

(defn get-ngrams-ending-with
  [ngrams target]
  (let [n (count (ffirst ngrams))
        target (if-not (seq? target) (seq (vector target)) target)]
    (if (zero? (count target))
      ngrams
      (filter #(= target (drop (- n (count target)) (first %))) ngrams))))

(defn perplexity
  [language ngrams]
  (Math/pow
    (reduce * (for [w language] (/
                                 (reduce + (map second (get-ngrams-ending-with ngrams w)))
                                 (reduce + (vals ngrams)))))
    (* -1 (/ 1 (count language)))))

(defn top-m-ngrams
  [terms m n]
  (let
    [n-grams (ngrams n terms)]
    (take m (keys
              (into (sorted-map-by (fn [key1 key2]
                                     (>= (get n-grams key1)
                                         (get n-grams key2))))
                    n-grams)))))
(defn ngram-model->top-m-ngrams
  [ngram-model m]
  (take m (into (sorted-map-by (fn [key1 key2]
                                 (>= (get ngram-model key1)
                                     (get ngram-model key2))))
                ngram-model)))

(defn document->ngrams
  "Takes a document (seq of textual units) and produces an ngram
   model."
  [document n]
  (apply merge-with + (map (partial ngrams n) (map util/tokenize-lower document))))

(defn corpus->ngrams
  "Takes a corpus as produced by reaper.corpus.load-corpus and computes
   an ngram map based on said corpus."
  [corpus n & {:keys [remove-stopwords
                      stem]
               :or {remove-stopwords false
                    stem false}}]
  (let [corpus (if remove-stopwords (corpus->stop-words corpus) corpus)
        corpus (if stem (corpus->stemmed-corpus corpus) corpus)]
    (->>
      (map #(document->ngrams % n) corpus)
      (apply merge-with +))))
