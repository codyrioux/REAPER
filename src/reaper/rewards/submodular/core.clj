(ns reaper.rewards.submodular.core
  (:require [reaper.rewards.util :refer :all]
            [reaper.util :as util]
            [opennlp.nlp :as nlp]
            [reaper.tools.porter-stemmer :refer [stem]]
            [reaper.tools.wordnet :as wordnet]))

(defn- make-ner-pipeline
  "Takes a list of models and creates a named entity extractor for each.
   Returns a function that performs extraction for all of the models at once."
  [models]
  (let [extractors
        (doall (map nlp/make-name-finder models))]
    (fn [textual-units]
      (flatten
        (map (fn [tu] (map #(% (util/tokenizer tu)) extractors))
             textual-units)))))

(defn make-ner-diversity-fn
  "Extracts named entities from the corpus and then scores a summary based
   on how many of the named entities it covers.
   Currently works on Person names but the usage of time, date, place, 
   organization and money are all possible."
  [corpus alpha]
  (let [ner (make-ner-pipeline
              ["resources/models/name/en-ner-person.bin"
               "resources/models/name/en-ner-location.bin"])
        entities (frequencies
                   (flatten
                     (ner (flatten corpus))))]
    (make-diversity-fn entities (fn [s] (flatten (ner (first s)))))))

(defn make-event-diversity-fn
  "Extracts events (verbs) from the corpus and then scores a summary based
   on how many of the events it covers."
  [corpus alpha & {:keys [remove-events]
                   :or {remove-events []}}]
  (let
    [pos-tagger (nlp/make-pos-tagger "resources/models/en-pos-maxent.bin")
     verb-tags #{"VB" "VBG" "VBN" "VBD" "VBP" "VBZ"}
     event-extractor (fn [corpus]
                       (->>
                         corpus
                         flatten
                         (map #(pos-tagger (util/tokenizer %)))
                         (apply concat)
                         (filter #(some (set [(second %)]) verb-tags))
                         (map first)
                         (map clojure.string/lower-case)
                         (remove #(some (set [%]) remove-events))))]
    (make-diversity-fn (frequencies (event-extractor corpus))
                       (fn [s] (event-extractor (first s))))))

(defn make-query-diversity-fn
  "Extracts words from the summary and scores based on overlap with the query
   synset according to wordnet."
  [wordnet-path query alpha]
  (let [synset (map stem (wordnet/query->synset wordnet-path query))]
    (make-diversity-fn (frequencies synset)
                       (fn [s] (map stem (flatten (map util/tokenizer (first s)))))) ))
