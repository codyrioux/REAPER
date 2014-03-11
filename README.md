# Citing REAPER

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

#Components
This library breaks the problem down into four major components:

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
of Lin and Blimes 2011 paper on submodular 

## Usage
See `reaper.experiments.sample.clj` for sample usage constructing an entire experiment. Alternately you can run the
sample from the command line using the uberjar:

```bash
java -jar reaper.jar <docs-path> <wordnet-path> <word-limit> <lambda>  

# For example on my machine
java -jar repaer.jar "dataset/D0745J/" "/usr/share/wordnet/" 250 6 
```

## License

Copyright © 2013 Cody Rioux
Distributed under the Eclipse Public License, the same as Clojure.
All code belonging to to other authors is released under their respective licenses.
