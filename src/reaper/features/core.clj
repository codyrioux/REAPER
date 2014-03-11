(ns reaper.features.core
  "Namespace for providing universal functions for working
   with features and sets of features.
   A feature set is considered to be a collection of fns
   that operate individually on a summary to extract a feature,
   these fns are known as a feature."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]))

(defmacro deffeatureset
  "Supplied with a name and any number of fns that accept a summary
   and produce a feature, or seq of features creates a fn to accept
   a summary and extract all features.
   Didn't need to be a macro, but syntactically it makes sense."
  [fname & features]
  (let [param (gensym)]
  `(defn ~fname [~param] 
     (flatten (map #(% ~param) [~@features])))))
