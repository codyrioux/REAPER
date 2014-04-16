(ns reaper.features.ngrams
  (:require
    [reaper.util :as util]
    [reaper.tools.stop-words :refer [tokens->stop-words]]
    [reaper.tools.porter-stemmer :refer [tokens->stemmed-tokens]]
    [reaper.tools.ngrams :refer [corpus->ngrams ngram-model->top-m-ngrams]]))

(defn make-ngram-vectorizer
  [corpus n m & {:keys [remove-stopwords
                        stem
                        binary
                        ]
                 :or {remove-stopwords false
                      stem false
                      binary false}}]
  (let
    [ngram-model (corpus->ngrams corpus n
                                 :remove-stopwords remove-stopwords
                                 :stem stem)
     top-m-ngrams (ngram-model->top-m-ngrams ngram-model m)
     ngram->idx (zipmap (keys top-m-ngrams) (range m)) ]
    (fn [s]
      (let
        [ngrams (corpus->ngrams [[s]] n :remove-stopwords remove-stopwords :stem stem)]
        (reduce #(assoc %1 (ngram->idx %2) (if binary 1 (get ngram-model %2 0.0)))
                (vec (repeat m 0))
                (filter #(some #{%} (keys ngram->idx)) (keys ngrams)))))))
