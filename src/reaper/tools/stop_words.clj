(ns reaper.tools.stop-words
  (:require [opennlp.nlp :as nlp]
            [clojure.java.io :as io]
            [reaper.util :refer [stop-words detokenize tokenizer]]))

(defn- remove-stop-words [s]  (remove stop-words (map clojure.string/lower-case (tokenizer s))))

(defn corpus->stop-words
  "Converts a corpus into one with stop-words removed. "
  [corpus]
  (map (fn [doc] (map #(detokenize (remove-stop-words %)) doc)) corpus))

(defn tokens->stop-words
  "Removes stop words from a collection of tokens."
  [tokens]
  (remove stop-words tokens))
