(ns reaper.tools.wordnet
  "A wrapper over the clj-wordnet wrapper developed and maintained
   by Delver.

   This namespace exists primarily to draw REAPER users' attention to the
   wordnet functionality contained within clj-wordnet. For full docs
   please refer to https://github.com/delver/clj-wordnet
   
   Any wordnet related REAPER utility functions will also be contained in
   this namespace, though bear in mind any major contributions should most
   definitely be aliased here and contributed upstream to the clj-wordnet
   library."
  (:require 
    [clj-wordnet.core :as wn]
    [opennlp.nlp :as nlp]
    [reaper.tools.stop-words :as sw]
    [reaper.util :as util]
    [reaper.tools.porter-stemmer :refer [tokens->stemmed-tokens]]
    [clojure.string :refer [lower-case]]))

(def make-dictionary wn/make-dictionary)
(def related-synsets wn/related-synsets)
(def related-words wn/related-words)

(def pos-tagger (nlp/make-pos-tagger (clojure.java.io/resource "models/en-pos-maxent.bin")))

(defn word->synset-lemmas
  [word pointers]
  (flatten (for [pointer pointers]
             (map :lemma (flatten (vals (related-synsets word pointer)))))))

(def ^:private
  opennlp-pos-to-wordnet
  {"NN" :noun
   "NNS" :noun
   "NNS$" :noun
   "NP" :noun
   "NP$" :noun
   "NPS" :noun
   "NPS$" :noun
   "NNP" :noun
   "VB" :verb
   "VBG" :verb
   "VBD" :verb
   "VBN" :verb
   "VBP" :verb
   "VBZ" :verb})

(defn query->synset
  "Provides a synonym expansion of all non-stopwords in the query.

   This process maps POS tags from opennlp to wordnet :verb and :noun
   for now it only concerns itself with verbs and nouns, perhaps
   in the future we will expand this.

   Information on WordNet pointers can be found at this URI:
   http://projects.csail.mit.edu/jwi/api/edu/mit/jwi/item/Pointer.html"
  [path-to-dict query & {:keys [stem remove-stopwords]
                         :or {stem false
                              remove-stopwords false}}]
  (let
    [wordnet (wn/make-dictionary path-to-dict)
     tokens (util/tokenizer query)
     pos-tags (into {} (map #(vec [(lower-case (first %)) (second %)])
                           (pos-tagger tokens)))
     tokens (if remove-stopwords (map lower-case (remove util/stop-words tokens)) (map lower-case tokens))
     tokens (filter #(some #{(pos-tags %)}
                           (set (keys opennlp-pos-to-wordnet))) tokens)
     words (apply concat (map #(wordnet %
                                        (get opennlp-pos-to-wordnet
                                             (get pos-tags %))) tokens))
     synset (flatten (map
                       (fn [x] (clojure.string/split x #"[-_ ]"))
                       (flatten (map #(word->synset-lemmas
                                        %
                                        [:hypernym
                                         :verb-group
                                         :hyponym])
                                     words))))]
    (if stem (tokens->stemmed-tokens synset) synset)))

(defn query->synset-str [path query & {:keys [stem remove-stopwords]
                                       :or {stem false remove-stopwords false}}]
  (clojure.string/join " " (query->synset path query :stem stem :remove-stopwords remove-stopwords)))
