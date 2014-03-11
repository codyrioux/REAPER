(ns reaper.tools.stop-words
  (:require [opennlp.nlp :as nlp]
            [clojure.java.io :as io]))

(def stop-words (set (line-seq (io/reader "resources/smart_common_words.txt"))))
(def ^:private tokenizer  (nlp/make-tokenizer "resources/models/en-token.bin"))
(defn- remove-stop-words [s]  (remove stop-words (map clojure.string/lower-case (tokenizer s))))
(def ^:private detokenize (nlp/make-detokenizer "resources/models/english-detokenizer.xml"))

(defn corpus->stop-words
  "Converts a corpus into one with stop-words removed. "
  [corpus]
  (map (fn [doc] (map #(detokenize (remove-stop-words %)) doc)) corpus))

(defn tokens->stop-words
  "Removes stop words from a collection of tokens."
  [tokens]
  (remove stop-words tokens))
