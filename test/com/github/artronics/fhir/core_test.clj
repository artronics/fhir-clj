(ns com.github.artronics.fhir.core-test
  (:require [clojure.test :refer :all]
            [com.github.artronics.fhir.core :refer :all]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]))

(def sample-file (-> "test/MedicationRequest.json" io/resource io/file))
(def mr-file (-> "test/StructureDefinition-MedicationRequest.json" io/resource io/file))
(def snapshot-file (-> "test/medication-request-snapshot.json" io/resource io/file))
(def differential-file (-> "test/medication-request-differential.json" io/resource io/file))
(def mr-schema (json/read-str (slurp mr-file) :key-fn keyword))
(def differential (json/read-str (slurp differential-file) :key-fn keyword))
(def snapshot (json/read-str (slurp snapshot-file) :key-fn keyword))

(def sn [{:id 1 :a "a"} {:id 2 :b "b"} {:id 3 :c "c"}])
(def df [{:id 4 :d "d" :x "x"} {:id 2 :b "bb"} {:id 3} {:id 4 :e "e"}])

;; table [scenarios]
;; one-scenario: [[sn][df][exp-fields]]
(def table
  [;; Edge cases with nil or empty coll
   [nil nil []] [nil [] []] [[] nil []] [[] [] []]
   ;; Empty list and data
   [[]
    [{:id 1 :a "a"}]
    [{:id 1 :a "a"}]]

   [[{:id 1 :a "a"}]
    []
    [{:id 1 :a "a"}]]
   ;; Differentials override snapshots
   [[{:id 1 :a "a"} {:id 2 :b "b"}]
    [{:id 1 :a "ax" :x "x"}]
    [{:id 1 :a "ax" :x "x"} {:id 2 :b "b"}]]
   ;; Merge everything
   [[{:id 1 :a "a"}]
    [{:id 2 :b "b"} {:id 3 :c "c"}]
    [{:id 1 :a "a"} {:id 2 :b "b"} {:id 3 :c "c"}]]

   ;; Preserve snapshot differences (NOTE: I'm not sure if this should happen in FHIR)
   [[{:id 1 :a "a" :b "b"}]
    [{:id 1 :a "ax"}]
    [{:id 1 :a "ax" :b "b"}]]])
(deftest snapshot2
  (testing "snapshot"
    (doseq [[sn df exp] table]
      (is (= exp (create-fields sn df))))))

(defn resource? [schema]
  (and (= (:resourceType schema) "StructureDefinition")
       (= (:kind schema) "resource")))

(s/def ::not-blank #(not (clojure.string/blank? %)))
(s/def ::id (s/and string? ::not-blank))
(s/def ::MedicationRequest (s/keys :req-un [::id]))

(defn write-file [schema] (spit "out.json" (json/write-str schema)))
