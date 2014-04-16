(ns reaper.experiments.sample
  "A rather complex sample experiment baed on Lin and Bilmes 2011."
  (:require [reaper.experiments.core :refer :all]
            [reaper.corpus :as corpus]
            [reaper.algorithms.greedy :refer [greedy-knapsack]]
            [reaper.features.tfidf :refer [make-tfidf-vectorizer make-ngram-tfidf-vectorizer]]
            [reaper.features.ngrams :refer [make-ngram-vectorizer]]
            [reaper.tools.wordnet :refer [query->synset-str]]
            [reaper.rewards.tfidf :refer [make-tfidf-sim make-tfidf-corpus-sim make-l1]]
            [reaper.rewards.submodular.sso :refer [make-sso-fn]]
            [reaper.tools.lcs :refer [make-lcs-reward]]
            [reaper.preprocessors.corpus :as cpp]
            [reaper.util :as util]
            [clojure.core.memoize :as memo]
            [reaper.rewards.submodular.rouge :as rouge]
            [reaper.rewards.util :as rwutil]
            [plumbing.core :refer :all]
            [plumbing.graph :as graph])
  (:gen-class))

(defn make-unigram-bigram-vectorizer
  [corpus m]
  (let
    [unigram-vectorizer (make-ngram-tfidf-vectorizer corpus 1 m)
     bigram-vectorizer (make-ngram-tfidf-vectorizer corpus 2 m)]
    (fn [x]
      (concat (unigram-vectorizer x) (bigram-vectorizer x)))))

(defn make-qsim
  "Counts the number of terms that overlap (up to bigram)
   from sentence x to the query string.
   TODO: Sentence expansion happens here."
  [query]
  (let
    [ug-vec (make-ngram-vectorizer [[query]] 1 50)
     bg-vec (make-ngram-vectorizer [[query]] 2 50)]
    (fn [x]
      (reduce + (concat (ug-vec x) (bg-vec x))))))

(def experiment-graph
  "A prismatic graph specifying the experiment computations."
  {;; Preprocessing
   :corpus (fnk [path] (->> (corpus/load-corpus path :match #"^(APW|NYT|XIE).*")
                            cpp/remove-quotes 
                            cpp/remove-brackets
                            (cpp/remove-short-sentences 10)))
   :actions (fnk [corpus] (map #(vec [:insert %]) (flatten corpus)))
   :sp (fnk [actions length-lim] (partial sp actions length-lim))
   :query (fnk [path wn-path] (slurp (str path "/query.txt")))
   ;; Score Functions
   :vectorizer (fnk [corpus] (memo/fifo  (make-unigram-bigram-vectorizer corpus 100) :fifo/threshold (count (flatten corpus))))
   :sim (fnk [corpus] (memo/fifo (make-tfidf-sim corpus 100 :remove-stopwords true :stem true)
                                 :fifo/threshold (* (count (flatten corpus)) (count (flatten corpus)))))
   :csim (fnk [corpus] (memo/fifo  (make-tfidf-corpus-sim corpus 100 :remove-stopwords true :stem true)
                                  :fifo/threshold (count (flatten corpus))))
   :qsim (fnk [corpus query] (memo/fifo  (make-qsim query)
                                  :fifo/threshold (count (flatten corpus))))
   :diversity (fnk [corpus csim vectorizer query qsim]
                   (rwutil/make-diversity-fn corpus csim vectorizer (quot (count (flatten corpus)) 5) :qsim qsim))
   :l1 (fnk [corpus sim csim] (make-l1 corpus sim csim 100))
   :reward(fnk [l1 diversity lambda]
                     (fn [s] (+ (l1 s) (* lambda (diversity s)))))
   ;; Computations
   :summary (fnk [reward length-lim sp]
                      (greedy-knapsack [[] [] 0]
                                       terminal?
                                       cost
                                       m 
                                       sp
                                       reward
                                       length-lim))
   :text(fnk [summary] (clojure.string/join "\n" (first summary)))})

(defn -main
  [path wn-path length-lim lambda]
  (let
    [summary-graph (graph/lazy-compile experiment-graph)
     output (summary-graph {:path path
                            :wn-path wn-path
                            :length-lim (if (string? length-lim)
                                          (read-string length-lim)
                                          length-lim)
                            :lambda (if (string? lambda)
                                      (read-string lambda)
                                     lambda)})]
    (println (:text output))))
