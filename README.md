# REAPER
REAPER was originally an acronym describing the system, it has however changes so much from its inception
that the acronym no longer applies.

REAPER is a clojure library implementing multi-document summarization functions, and it is
maintained by the University of Lethbridge Natural Language Processing lab. REAPER aims to
support multiple goals such as general summarization, query-focused summarization, complex question
answering, etc...  It was originally written as an implementation for a paper submission

The focus of my work, and thus focus of the library is on summarization through submodular maximization,
though the library is by no means limited to such, and it contains algorithms not typically used for this.

This is my first attempt at maintaining an open source library that I actually expect others use, so
feedback is encouraged via github issues, but be gentle!

## Using REAPER

Include the following in your `project.clj` dependencies:

```clojure
[reaper "0.1.0-SNAPSHOT"]
```

Now lets create a namespace to perform a very small summarization task.

```clojure
(ns sample.core
  (:require [reaper.experiments.core :refer :all]
            [reaper.algorithms.greedy :refer :all]
            [reaper.rewards.submodular.rouge :refer [make-rouge-n]])
  (:gen-class))

(defn -main
  [length-lim]
  (let
    [corpus [(line-seq (java.io.BufferedReader. *in*))]
     actions (map #(vec [:insert %]) (flatten corpus))
     sp (partial sp actions (read-string length-lim))
     reward (make-rouge-n corpus 2)]
    (println (clojure.string/join "\n"
                                  (first
                                    (greedy-knapsack
                                      [[] [] 0]
                                      terminal?
                                      cost
                                      m
                                      sp
                                      reward
                                      (read-string length-lim)))))))
```

Now we can simply comple, call the jar file with a length limit and feed the text in (one textual unit per line) on standard input. The computed summary will be printed to standard output.
Here we feed in the first two paragraphs of the wikipedia article on automatic summarization, providing the EOF marker at the end with CTRL+D. No blank lines please, unless you filter them
out of your line-seq.

```bash
lein clean && lein uberjar
java -jar target/sample-0.1.0-SNAPSHOT-standalone.jar 50
Automatic summarization is the process of reducing a text document with a computer program in order to create a summary that retains the most important points of the original document.
As the problem of information overload has grown, and as the quantity of data has increased, so has interest in automatic summarization.
Technologies that can make a coherent summary take into account variables such as length, writing style and syntax.
An example of the use of summarization technology is search engines such as Google.
Document summarization is another.
Generally, there are two approaches to automatic summarization: extraction and abstraction.
Extractive methods work by selecting a subset of existing words, phrases, or sentences in the original text to form the summary.
In contrast, abstractive methods build an internal semantic representation and then use natural language generation techniques to create a summary that is closer to what a human might generate.
Such a summary might contain words not explicitly present in the original. The state-of-the-art abstractive methods are still quite weak, so most research has focused on extractive methods.

```

This returns the following summary:

Automatic summarization is the process of reducing a text document with a computer program in order to create a summary that retains the most important points of the original document.
Technologies that can make a coherent summary take into account variables such as length, writing style and syntax.

You can find the code for this sample in `reaper.experiments.simple.clj` and the code for a more complex example that uses many more components of the system in `reaper.experiments.sample.clj`.


## Citing REAPER

I'm an academic, and as you may know we're always desperately clawing at citations. If you use REAPER for a
publication please cite me. I have provided BiBTeX below.

```latex
@misc{Rioux2014,
  author = {Cody Rioux},
  title = {REAPER},
  year = {2014},
  publisher = {GitHub},
  journal = {GitHub repository},
  howpublished = {\url{https://github.com/codyrioux/reaper}},
  commit = {<COMMIT HASH HERE>}
}
```


#Components
This library breaks the problem down into several major components, each of which are detailed in this section.

## Corpus
The corpus is implemented as a seq of documents, which are a seq of textual units. In the case of REAPER
textual units are sentenecs, and it has been designed with that in mind. That said there is no reason this
has to be the only case.

The prmary function for loading a corpus is `reaper.corpus/load-corpus` which takes an optional named
regex parameter called `:match`to filter out unwanted files.

```clojure
(def corpus (reaper.corpus/load-corpus "/path/to/documents/" :match #"^DOC.*")

```

## Preprocessors
Preprocessors are functions that operate on the corpus to transform it in some way. This includes
removing quotes, or short sentences for example. They will always return a valid corpus.

```clojure
(->> corpus
  (reaper.preprocessors.corpus/remove-short-sentences 10
  (reaper.preprocessors.corpus/remove-quotes))
```

## Feature Extractors
Feature extractors generate either a single or vector of features from a summary. In this way we can generate abstract
representations of summaries for algorithms such as the k-means clustering algorithm. Furthermore many machine learning
algorithms require a feature extraction step. In the `reaper.features.core` namespace exists a `deffeatureset` macro
which takes a series of feature extractors and compresses their output into a single vector, useful for combining
vectorization and linguistic feature extractors for example.


## Rewards 
Reward functions are generally the function in which we are attempting to optimize.


## Algorithms
Algorithms are used to optimize the aforementioned reward functions. REAPER contains (in varying levels of quality)
implementations of: td(lambda), q-learning, sarsa, k-means clustering, greedy, and greedy with a knapsack constraint.
All of these algorithms can be used to optimize score functions for summairzation or be used to support further
parts of the summarization process. The sample for example uses clustering not to optimize the score function but to
generate disjoint sets for diversity.

## Experiments

These components are tied together in an `experiment` of which you can find a sample implementation
in the `reaper.experiments.sample` namespace. This is a rough, but not completely accurate reimplementation
of Lin and Blimes 2011 paper on summarization through submodular maximization.


## License

Copyright © 2013 Cody Rioux
Distributed under the Eclipse Public License, the same as Clojure.
All code belonging to to other authors is released under their respective licenses.
