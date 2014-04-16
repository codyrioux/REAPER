(ns reaper.tools.tfidf
  "A term-frequency inverse-document-frequency implementation in idiomatic clojure.
   A term is a single token, doc is a seq of terms and docs is a seq of docs."
  (:require
    [reaper.tools.stop-words :refer [corpus->stop-words]]
    [reaper.tools.porter-stemmer :refer [corpus->stemmed-corpus]]
    [reaper.tools.ngrams :refer [ngrams]]
    [reaper.util :as util]))

(defn- log [x] (Math/log x))
(defn- t-in-d? [t, d] (some #{t} d))
(defn- f [t doc] (count (filter #(= t %) doc)))

(defn tf [doc-to-max-f t doc] (+ 0.5 (/ (* 0.5 (f t doc)) (doc-to-max-f doc))))
(defn idf [t docs] (log (/ (count docs) (count (filter (partial t-in-d? t) docs)))))
;(defn tfidf [1t d docs] (* (tf t d) (idf t docs)))

(defn corpus->tfidf-model
  "Converts the provided corpus into a map of token => tf*idf value."
  [corpus & {:keys [remove-stopwords
                    stem]
             :or {remove-stopwords false
                  stem false}}]
  (let
    [corpus (if remove-stopwords (corpus->stop-words corpus) corpus)
     corpus (if stem (corpus->stemmed-corpus corpus) corpus)
     corpus (map (fn [doc] (filter #(not (empty? %)) doc)) corpus)
     terms (distinct (flatten (map util/tokenize-lower (flatten corpus)))) 
     docs (map (fn [doc] (flatten (map util/tokenize-lower doc))) corpus)
     docs (filter #(not (empty? %)) docs)
     doc-to-max-f (zipmap docs (map #(apply max (map (fn [t] (f t %)) %)) docs))
     tf-map (apply merge-with + (for [d docs] (zipmap terms (map #(tf doc-to-max-f % d) terms))))
     idf-map (if (<= 1 (count docs))
               (zipmap terms (repeat 1))
               (zipmap terms (map #(idf % docs) terms)))]
    (zipmap terms (map #(*
                         (get tf-map %)
                         (get idf-map %))
                       terms))))

(defn tfidf-model->top-n-terms
  "Produces a seq of the top n terms as dictated by their tf*idf scores."
  [tfidf-model n]
  (take n (keys
            (into (sorted-map-by (fn [key1 key2]
                                   (<= (get tfidf-model key2)
                                       (get tfidf-model key1))))
                  tfidf-model))))

(defn corpus->ngram-tfidf-model
  "Converts the provided corpus into a map of ngram => tf*idf value."
  [corpus n &  {:keys [remove-stopwords
                    stem]
             :or {remove-stopwords false
                  stem false}}]
  (let
    [corpus (if remove-stopwords (corpus->stop-words corpus) corpus)
     corpus (if stem (corpus->stemmed-corpus corpus) corpus)
     corpus (map (fn [doc] (filter #(not (empty? %)) doc)) corpus)
     docs (map (fn [doc] (apply concat (map #(partition n 1 %) (map util/tokenize-lower doc)))) corpus)
     terms (distinct (apply concat docs))
     docs (filter #(not (empty? %)) docs)
     doc-to-max-f (zipmap docs (map #(apply max (map (fn [t] (f t %)) %)) docs))
     tf-map (apply merge-with + (for [d docs] (zipmap terms (map #(tf doc-to-max-f % d) terms))))
     idf-map (if (<= 1 (count docs))
               (zipmap terms (repeat 1))
               (zipmap terms (map #(idf % docs) terms)))]
    (zipmap terms (map #(*
                         (get tf-map %)
                         (get idf-map %))
                       terms))))
