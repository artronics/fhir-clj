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

;;(deftest my-test
;;  (testing "StructureDefinition"
;;    (let [res (slurp mr-file)]
;;      (def data (json/read-str (slurp mr-file) :key-fn keyword)))

(defn resource? [schema]
  (and (= (:resourceType schema) "StructureDefinition")
       (= (:kind schema) "resource")))

(s/def ::not-blank #(not (clojure.string/blank? %)))
(s/def ::id (s/and string? ::not-blank))
(s/def ::MedicationRequest (s/keys :req-un [::id]))

(defn cardinality [element]
  (let [[base-min base-max derived-min derived-max]
        [(:min element) (:max element) (:min (:base element)) (:max (:base element))]]
    (case [base-min base-max derived-min derived-max]
      [0 "1", 0 "1"] :optional
      [0 "1", 0 0] :not-used
      [0 "1", "1" "1"] :required

      [0 "*", 0 0] :not-used
      [0 "*", 0 "1"] :optional
      [0 "*", 0 "*"] :optional-many
      [0 "*", "1" "1"] :required
      [0 "*", "1" "*"] :at-least-1

      ["1" "1", "1" "1"] :required

      ["1" "*", "1" "1"] :required
      ["1" "*", "1" "*"] :at-least-1)))

(defn fields [schema]
  (let [name (:id schema)
        elements (get-in schema [:snapshot :element])
        field? (fn [element] (string/starts-with? (:path element) (str name ".")))
        field-from-path (fn [element] (string/replace (:path element) (re-pattern (str name ".")) ""))
        add-field-name (fn [element] (assoc element :field (field-from-path element)))
        add-cardinality (fn [element] (assoc element :cardinality (cardinality element)))]
    (map (comp add-cardinality add-field-name) (filter field? elements))))

(defn add-fields [schema]
  (let [fields (fields schema)]
    (assoc schema :fields fields)))

(defn diff-snapshot [schema]
  (let [snapshot (get-in schema [:snapshot :element])
        differential (get-in schema [:differential :element])
        eq-id? (fn [d s] (= (:id d) (:id s)))
        applied-diff (for [d differential s snapshot :when (eq-id? d s)] (merge s d))]
    (merge snapshot applied-diff)))


(def sn [{:id 1 :a "a"} {:id 2 :b "b"} {:id 3 :c "c"}])
(def df [{:id 4 :d "d" :x "x"} {:id 2 :b "bb"} {:id 3}])

(defn eq-id? [a b] (= (:id a) (:id b)))
(defn p [sx] (fn [dx] (if (eq-id? sx dx)
                        [(merge sx dx), dx]
                        [sx, nil])))

(defn sn-df [sn df]
  (let [eq-id? (fn [a b] (= (:id a) (:id b)))]
    (loop [fields [], sn sn, df df]
      (let [sn0 (first sn)
            sn-x (rest sn)
            apply-diff (fn [df-i] (when (eq-id? sn0 df-i) [(merge sn0 df-i), df-i]))
            [fields df] (if-let [[field consumed-df] (some apply-diff df)]
                          [(cons field fields) (remove #(= % consumed-df) df)]
                          [(cons sn0 fields) df])]

        (if (seq sn-x)
          (recur fields sn-x df)
          (concat fields df))))))



(defn apply-diff
  [schema]
  (let [snapshots (get-in schema [:snapshot :element])
        differentials (get-in schema [:differential :element])
        eq-id? (fn [d s] (= (:id d) (:id s)))
        find-first (fn [d s] (if (eq-id? d s) d nil))
        f (fn [fields snapshot] (let [merged (reduce (fn [diffed] ()) [] differentials)]
                                  (merge fields (first merged))))]
    ()))



(defn write-file [schema] (spit "out.json" (json/write-str schema)))

(defmacro resource-spec [schema]
  (let [fields (fields schema)]))





(def ex-res {:id "foo"})

(defn validate [schema])
