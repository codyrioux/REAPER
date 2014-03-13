(ns reaper.util
  (:require [clojure.zip :as zip]
            [opennlp.nlp :as nlp]
            [clojure.string :as str]
            [clojure.set]
            [clojure.core.async :refer [>! <! <!! chan go-loop go to-chan]]))

(defn sink
  "Returns an atom containing a vector. Consumes values from channel
   ch and conj's them into the atom."
  [ch]
  (let [a (atom [])]
    (go-loop []
             (let [val (<! ch)]
               (when-not (nil? val)
                 (swap! a conj val)
                 (recur))))
    a))

(defn parallel
  "Processes values from input channel in parallel on n 'go' blocks.

   Invokes f on values taken from input channel. Values returned from f
   are written on output channel.

   Returns a channel which will be closed when the input channel is
   closed and all operations have completed.

   Note: the order of outputs may not match the order of inputs."
  [n f input output]
  (let [tasks (doall
                (repeatedly n
                            #(go-loop []
                                      (let [in (<! input)]
                                        (when-not (nil? in)
                                          (let [out (f in)]
                                            (when-not (nil? out)
                                              (>! output out))
                                            (recur)))))))]
    (go (doseq [task tasks]
          (<! task)))))

(defn argmax
  "Returns the item in coll which results in the maximal value for f."
  [f coll]
  (if (empty? coll) nil
    (let [results (zipmap coll (map f coll))
          max-value (apply max (vals results))
          max-args (map first (filter #(= max-value (second %)) results))]
      (rand-nth max-args))))

(defn pargmax
  "Returns the item in coll which results in the maximal value for f.
   Computations are performed in parallel."
  [f coll]
  (if (empty? coll) nil
    (let [input (to-chan coll)
          output (chan)
          results (sink output)
          _ (<!! (parallel
                   (.availableProcessors (Runtime/getRuntime))
                   (fn [x]
                     (vec [x (f x)]))
                   input
                   output))
          results (into {} @results)
          max-value (apply max (vals results))
          max-args (map first (filter #(= max-value (second %)) results))]
      (rand-nth max-args))))


(defn argmin
  "Returns the item in coll which results in the minimal value for f."
  [f coll]
  (when (seq coll)
    (let [results (zipmap coll (map f coll))
          min-value (apply min (vals results))
          min-args (map first (filter #(= min-value (second %)) results))]
      (rand-nth min-args))))


(defn fmap 
  "Maps f to each value of m, returning the corresponding map."
  [f m]
  (into  {} (for  [[k v] m]  [k  (f v)])))

(defn fkmap
  [f m]
  (into {} (for [[k v] m] [k (f k v)])))

(defn rand-from-probdist
  [probdist]
  (loop
    [r (rand)
     coll probdist]
    (cond
      (= 1 (count coll))
      (ffirst coll) 
      (> 0 (- r (second (first coll))))
      (ffirst coll) 
      :else
      (recur
        (- r (second (first coll)))
        (rest coll)))))

(defn normalize-map-weights
  [m]
  (let [m-sum (reduce + (vals m))]
    (fmap #(/ % m-sum) m)))

(defn all-before
  "Returns a list of elements in a collection before the specified item.
   Returns the whole collection if item is not present."
  [coll item]
  (loop [coll coll
         accum []]
    (cond
      (= (first coll) item) accum
      (empty? coll) accum
      :else (recur (rest coll) (conj accum (first coll))))))

(defn- doc-contains?  [x doc] (some (partial = x) doc))

(defn str-contains?
  [word text]
  (try
    (not (nil? (re-find (re-pattern word) text)))
    (catch Exception e false)))

(defn pos
  "Returns the position of x in the sequence. 1 based."
  [docs x]
  (let [parent (first (filter (partial doc-contains? x) docs))]
    (inc (count (all-before parent x)))))

(def stop-words (set (line-seq (clojure.java.io/reader (clojure.java.io/resource "smart_common_words.txt")))))
(def tokenizer  (nlp/make-tokenizer (clojure.java.io/resource "models/en-token.bin")))
(defn tokenize-lower [s] (map str/lower-case (tokenizer s)))
(def detokenize (nlp/make-detokenizer (clojure.java.io/resource "models/english-detokenizer.xml")))
