;   Copyright (c) 2011, Erik Soehnel All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;;; longest common subsequence implementation as described in:
;;; http://en.wikipedia.org/wiki/Longest_common_subsequence_problem

(ns reaper.tools.lcs
  (:require [reaper.util :as util]
            [reaper.tools.stop-words :refer [tokens->stop-words corpus->stop-words]]
            [reaper.tools.porter-stemmer :refer [tokens->stemmed-tokens corpus->stemmed-corpus]]))

(defn- wlcs
  [^objects a1 ^objects a2]
  (let [a1-len (alength a1)
        a2-len (alength a2)
        prev (int-array (inc a2-len))
        curr (int-array (inc a2-len))]
    (loop [i 0 max-len 0 prev prev curr curr]
      (if (< i a1-len)
        (recur (inc i)
               (long (loop [j 0 max-len max-len]
                 (if (< j a2-len)
                   (if (= (aget a1 i) (aget a2 j))
                     (let [match-len (inc (aget prev j))]
                       (do
                         (aset curr (inc j) match-len)
                         (recur (inc j) (max max-len match-len))))
                     (do
                       (aset curr (inc j) 0)
                       (recur (inc j) max-len)))
                   max-len)))
               curr
               prev)
        max-len))))

(defn lcs [x y] (wlcs (into-array x) (into-array y)))

(defn average [& nums] (/ (reduce + nums) (count nums))) 

(defn make-lcs-reward
  "Makes an LCS based reward function which calculates LCS(s, doc) for
   ever doc in corpus. Applies func to the result (default average)."
  [corpus & {:keys [func remove-stopwords stem]
             :or {func average
                  remove-stopwords false
                  stem false}}]
  (let
    [corpus (if remove-stopwords (corpus->stop-words corpus) corpus)
     corpus (if stem (corpus->stemmed-corpus corpus) corpus)
     docs (map #(apply concat (map util/tokenizer %)) corpus)]
    (fn [s]
      (let
        [s (apply concat (map util/tokenizer (first s)))
         s (if remove-stopwords (tokens->stop-words s) s)
         s (if stem (tokens->stemmed-tokens s) s)]
        (apply func (map #(lcs % s) docs)))))) 
