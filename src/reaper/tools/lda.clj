(ns reaper.tools.lda
  "A wrapper over the mallet library. Latent Dirichlet
   Allocation for topic modelling."
  (:import (cc.mallet.types InstanceList Instance))
  (:import (cc.mallet.pipe Pipe SerialPipes CharSequence2TokenSequence TokenSequence2FeatureSequence))
  (:import (cc.mallet.topics ParallelTopicModel))
  (:require [reaper.util :as util]))

(defn- document-to-instance
  "Convert a document ID and a text string to a MALLET instance"
  [[documentid text]]
  (Instance. text "nolabel" documentid nil))

(defn- get-instance-list
  "Convert (documentID, text) map to MALLET InstanceList"
  [documentmap]
  (let [pipes (new SerialPipes
                   [(new CharSequence2TokenSequence #"\S+")
                    (new TokenSequence2FeatureSequence)])]
    (doto (new InstanceList pipes)
      (.addThruPipe (.iterator (map document-to-instance documentmap))))))

(defn- get-feature-sequence
  [documentmap]
  (let [pipes (new SerialPipes
                   [(new CharSequence2TokenSequence #"\S+")
                    (new TokenSequence2FeatureSequence)])]
    (doto (new InstanceList pipes)
      (.addThruPipe (.iterator (map document-to-instance documentmap))))))

(defn run-lda
  "Train a ParallelTopicModel from a given InstanceList with named
   arguments for all LDA parameters"
  [instancelist & {:keys [T numiter topwords
                          showinterval numthreads
                          optinterval optburnin
                          usesymmetricalpha alpha beta
                          thetathresh thetamax]
                   :or {T 50 numiter 500
                        topwords 20 showinterval 50
                        numthreads
                        (dec (.availableProcessors
                               (Runtime/getRuntime)))
                        optinterval 25 optburnin 200
                        usesymmetricalpha false
                        alpha 50.0 beta 0.01
                        thetathresh 0.0 thetamax -1}}]
  (doto (new ParallelTopicModel T alpha beta)
    (.addInstances instancelist)
    (.setTopicDisplay showinterval topwords)
    (.setNumIterations numiter)
    (.setOptimizeInterval optinterval)
    (.setBurninPeriod optburnin)
    (.setSymmetricAlpha usesymmetricalpha)
    (.setNumThreads numthreads)
    (.estimate)))

(defn corpus->instances
  [corpus]
  (let
    [docs (zipmap (range (count corpus)) (map #(clojure.string/join " " %) corpus))]
    (get-instance-list docs)))

(defn lda->vectorizer
  [lda-model & {:keys [numiter
                       burnin]
                :or {numiter 500
                     burnin 200}}]
  (let
    [inferencer (.getInferencer lda-model)]
    (fn
      [sentence]
      (map identity (.getSampledDistribution inferencer 
                                             (first (get-instance-list {1 sentence}))
                                             numiter numiter burnin)))))

(defn corpus->lda
  "Creates an estimated ParallelTopicModel from a REAPER corpus."
  [corpus T]
  (run-lda (corpus->instances corpus) :T T))

(defn corpus->lda-vectorizer
  "Creates an LDA vectorizer from corpus with T topics."
  [corpus & {:keys [T] :or {T 100}}]
  (let
    [lda (corpus->lda corpus T)]
    (lda->vectorizer lda)))
