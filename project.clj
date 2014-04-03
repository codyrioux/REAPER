(defproject reaper "0.1.0-SNAPSHOT"
  :description "A project implementing automated summarization
                as a submodular maximization problem."
  :url "https://github.com/codyrioux/reaper"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clojure-opennlp "0.2.2"]
                 [hiccup "1.0.3"]
                 [clj-wordnet "0.0.5"]
                 [org.clojure/math.combinatorics "0.0.7"]
                 [cc.mallet/mallet "2.0.7"]
                 [incanter/incanter-core "1.5.0-SNAPSHOT"]
                 [criterium "0.4.3"]
                 [com.taoensso/timbre "2.6.1"]
                 [prismatic/plumbing "0.1.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/core.memoize "0.5.6"]]
  :main reaper.experiments.sample 
  :jvm-opts ["-Xmx2g" "-Xms2g" "-server"])
