(ns com.github.artronics.fhir.schema.processor
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [com.github.artronics.fhir.utils :refer
             [merge-elements sanitise-path elem->field path->keyword cardinality]]
            [datascript.core :as d]))

(def med-req
  (-> "test/StructureDefinition-MedicationRequest.json"
      io/resource
      io/file
      slurp
      (json/read-str :key-fn keyword)))

(defn merge-differential->snapshot
  [res]
  (let [snapshot (-> res :snapshot :element)
        differential (-> res :differential :element)
        merged (merge-elements snapshot differential)]
    (assoc res :merged-fields merged)))

(defn extract-fields [res]
  (let [fields (-> res merge-differential->snapshot :merged-fields)
        elems (map elem->field fields)]
    elems))

(defn extract-field2
  [element]
  (let [path (sanitise-path element)
        field-id (path->keyword path "")]
    {(keyword field-id)
     {:cardinality (cardinality element)}}))

(comment
  (def fields
    (-> (merge-differential->snapshot med-req)
        :merged-fields))
  (def elem (nth fields 9))
  (extract-field2 elem "")
  (first (extract-fields med-req))
  (select-keys (nth (extract-fields med-req) 0) [:path :field-name :cardinality]))
