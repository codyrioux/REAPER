(ns reaper.rewards.submodular.lcs
  (:require [reaper.tools.lcs :as lcs]))

(defn lcs
  [x y]
  (let [matrix (lcs/lcs-matrix x y)]
    (lcs/lcs-vec matrix x y)))

(defn rlcs
  "Computes the sentence-level LCS recall.
   Summaries x of length m, y of length n.
   Assuming x is the reference and y the candidate."
  [x y]
  (/ (count (lcs x y)) (count x)))

(defn plcs [x y] (/ (count (lcs x y)) (count y)))

(defn flcs
  "ROUGE LCS F-measuse.
   x is a seq of tokens for a reference summary.
   y is a seq of tokens for a candidate summary.
   Beta is a balancing factor."
  [x y beta]
  (/
   (* (inc (* beta beta)) (rlcs x y) (plcs x y))
   (+ (rlcs x y) (* (* beta beta) (plcs x y)))))

(defn avg-flcs
  [X y beta]
  (/ (reduce + (map #(flcs % y beta) X))
     (count X)))
