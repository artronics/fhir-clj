(ns com.github.artronics.fhir.loader
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [>! <! >!! <!! go chan buffer close!]]))

(def fl ["foo.json" "bar.json"])

(defn load-resources
  [resources]
  (loop [rss resources]
    (when-let [rs (first rss)]
      (println rs)
      (recur (rest rss)))))

(comment
  (load-resources fl))

