(ns reaper.experiments.sample
  "Higher order functions designed to implement the experiment.
   These are intended to be generic enough to be passed to different
   learners and are intended to be partialed before being used."
  (:require [clojure.set :refer [difference]]
            [reaper.corpus :as corpus]
            [reaper.algorithms.greedy :refer [greedy-knapsack]]
            [reaper.tools.vectorization :refer [make-tfidf-vectorizer]]
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

;; Higher order experiment specific functions

(def cost-ignored-tokens #{"," "'" "." "(" ")"})
(defn cost [[action content]] (count (remove cost-ignored-tokens
                                             (util/tokenizer content))))
(defn length [s] (reduce + (map #(cost [:_ %]) (first s))))
(defn terminal? [s] (= 1 (nth s 2)))

(defn m
  "Takes a state and action and returns a new state and reward.
   s: State. In the form of a summary as in eacl2014.features (S, A, f, c)
   a: Action [:insert sentence-content]

   Return: A transitioned state."
  [s a]
  (cond
    (= (nth s 2) 1) s
    (nil? a) s
    (= :insert (first a))
    [(conj (first s) (second a)) (conj (second s) a) 0 0]
    (= :finish (first a))
    [(first s) (conj (second s) a) 1 0]))

(defn sp
  "Observes a state and determines a set of possible actions.
   Right now that just means allowing insertion of any sentence not currently
   already in the summary.

   actions: A list of possible actions.
   s: The current state as described in eacl2014.features (S, A, f)

   Returns: A list of potential actions in the form [:action-type string-describing-it]"
  [actions k s]
  (cond
    (< k (length s)) []
    (= (nth s 2) 0) (remove (set (second s)) actions)
    (= (nth s 2) 1) []))

;;
;; Specification of the actual experiment
;;

(def experiment-graph
  "A prismatic graph specifying the experiment computations."
  {;; Preprocessing
   :corpus (fnk [path] (->> (corpus/load-corpus path :match #"^(APW|NYT|XIE).*")
                            cpp/remove-quotes 
                            cpp/remove-brackets
                            (cpp/remove-short-sentences 10)))
   :actions (fnk [corpus] (map #(vec [:insert %]) (flatten corpus)))
   :sp (fnk [actions length-lim] (partial sp actions length-lim))
   :query (fnk [path wn-path] (query->synset-str wn-path (slurp (str path "/query.txt"))))
   ;; Score Functions
   :vectorizer (fnk [corpus] (make-tfidf-vectorizer corpus 100 :remove-stopwords true :stem true))
   :sim (fnk [corpus] (memo/fifo (make-tfidf-sim corpus 100 :remove-stopwords true :stem true)
                                 :fifo/threshold (* (count (flatten corpus)) (count (flatten corpus)))))
   :csim (fnk [corpus] (memo/fifo  (make-tfidf-corpus-sim corpus 100 :remove-stopwords true :stem true)
                                  :fifo/threshold (count (flatten corpus))))
   :diversity (fnk [corpus sim csim vectorizer query]
                   (rwutil/make-diversity-fn corpus sim csim vectorizer (quot (count (flatten corpus)) 5) :query query))
   :lcsmax (fnk [corpus] (make-lcs-reward corpus :remove-stopwords true :stem true :func max))
   :lcsavg (fnk [corpus] (make-lcs-reward corpus :remove-stopwords true :stem true))
   :l1 (fnk [corpus sim csim] (make-l1 corpus sim csim 100))
   :sso (fnk [corpus] (make-sso-fn corpus))
   :reward-orig (fnk [l1 diversity lambda]
                     (fn [s] (+ (l1 s) (* lambda (diversity s)))))
   :reward-max (fnk [l1 diversity lcsmax lambda]
                    (fn [s] (+ (l1 s) (* lambda (diversity s)) (lcsmax s))))
   :reward-avg (fnk [l1 diversity lcsavg lambda]
                    (fn [s] (+ (l1 s) (* lambda (diversity s)) (lcsavg s))))
   :reward-sso (fnk [l1 diversity sso lambda]
                    (fn [s] (+ (l1 s) (* lambda (diversity s)) (* 50 (sso s)))))
   ;; Computations
   :summary-orig (fnk [reward-orig length-lim sp]
                      (greedy-knapsack [[] [] 0]
                                       terminal?
                                       cost
                                       m 
                                       sp
                                       reward-orig
                                       length-lim))
   :summary-max (fnk [reward-max length-lim sp]
                     (greedy-knapsack [[] [] 0]
                                      terminal?
                                      cost
                                      m 
                                      sp
                                      reward-max
                                      length-lim))
   :summary-avg (fnk [reward-avg length-lim sp]
                     (greedy-knapsack [[] [] 0]
                                      terminal?
                                      cost
                                      m 
                                      sp
                                      reward-avg
                                      length-lim))
   :summary-sso (fnk [reward-sso length-lim sp]
                     (greedy-knapsack [[] [] 0]
                                      terminal?
                                      cost
                                      m 
                                      sp
                                      reward-sso
                                      length-lim))
   :text-orig (fnk [summary-orig] (clojure.string/join "\n" (first summary-orig)))
   :text-max (fnk [summary-max] (clojure.string/join "\n" (first summary-max)))
   :text-avg (fnk [summary-avg] (clojure.string/join "\n" (first summary-avg)))
   :text-sso (fnk [summary-sso] (clojure.string/join "\n" (first summary-sso)))})

(defn -main
  [path wn-path cluster length-lim lambda]
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
    (spit (str cluster ".original.system") (:text-orig output))
    (spit (str cluster ".lcsmax.system") (:text-max output))
    (spit (str cluster ".lcsavg.system") (:text-avg output))
    (spit (str cluster ".sso.system") (:text-sso output))))
