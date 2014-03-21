(ns reaper.corpus
  "This namespace defines functions for loading the corpus from disk.
   The results of functions in this namespace should be considered the
   standard for corpus representation throughout this software
   package."
  (:require [opennlp.nlp :as nlp]))

(def ^:private get-sentences (nlp/make-sentence-detector (clojure.java.io/resource "models/en-sent.bin")))

(defn load-corpus
  "Loads a corpus into a seq of documents, where a document is a seq of
   textual units, which are sentences in this case.
   A corpus is expected to be segmented into one textual unit per
   line."
  [path & {:keys [match
                  split]
           :or {match #".*"
                split false}}]
    (->>
      (rest (file-seq (clojure.java.io/file path)))
      (filter #(re-matches match (.getName %)))
      (map #(.getPath %))
      (map #(if split
              (get-sentences (slurp %))    
              (line-seq (clojure.java.io/reader %))))
      (map #(filter (fn [x] (not (empty? x))) %))
      (filter seq?)))
