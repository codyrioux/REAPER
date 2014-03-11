(ns reaper.tools.lda
  "A wrapper over the mallet-lda wrapper. Latent Dirichlet
   Allocation for topic modelling.
   
   This namespace exists primarily to draw users attention to the LDA
   functionality implemented by mallet-lda, as well as to implement utility
   functions associated with LDA.
   
   https://github.com/marcliberatore/mallet-lda"
  (:require [mallet-lda.core :as lda]
            [reaper.util :as util]))

(def lda lda)

(defn- corpus->lda-format
  [corpus]
  (map #(vec [%1 (flatten (map util/tokenizer %2))])
       (range 1 (inc (count corpus))) corpus))

(defn corpus->lda
  [corpus n-topics]
  (->>
    corpus
    corpus->lda-format
    lda/make-instance-list
    (lda :num-topics n-topics)))
