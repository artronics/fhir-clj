(ns com.github.artronics.fhir.utils
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [com.github.artronics.fhir.schema.specs :refer :all]))

(defn- eq-id? [a b] (= (:id a) (:id b)))

(defn- apply-diff [differential snapshots]
  "Find matching id in snapshots and merge it to differential to it.
  Returns [merged , snapshots minus matched] or
  identity if no match was found"
  (let [matched-sn? (fn [sn-i] (when (eq-id? differential sn-i) sn-i))
        matched-sn (some matched-sn? snapshots)]
    (if matched-sn
      [(merge matched-sn differential) (remove #(= % matched-sn) snapshots)]
      [differential snapshots])))

(defn- apply-diff-reducer-fn
  [[acc sn-x] df-i]
  (let [[merged sn-x] (apply-diff df-i sn-x)]
    [(conj acc merged) sn-x]))


(defn merge-elements [sn df]
  (let [[acc sn-x] (reduce apply-diff-reducer-fn [[] sn] df)]
    ;; concat whatever is left from snapshots i.e. the one that didn't match with any id in differentials.
    (concat acc sn-x)))

(defn- check-returns [rets sn df]
  (let [sc (count sn)
        dc (count df)
        rc (count rets)]
    (and (<= (max sc dc) rc (+ sc dc))
         (let [check-merge (fn [ret-i]
                             (let [find-eq (fn [nx] (first (filter #(eq-id? % ret-i) nx)))
                                   matched-sn (find-eq sn)
                                   matched-df (find-eq df)]
                               (= ret-i (merge matched-sn matched-df))))]
           (every? check-merge rets)))))

(s/fdef merge-elements
        :args (s/cat :snapshot :fhir.schema/element
                     :differentials :fhir.schema/element)
        :ret :fhir.schema/element
        :fn #(let [ret (-> % :ret)
                   sn (-> % :args :snapshot)
                   df (-> % :args :differentials)]
               (check-returns ret sn df)))

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

(defn add-cardinality [elem]
  (assoc elem :cardinality (cardinality elem)))

(defn add-field-name [elem, name]
  (let [path (:path elem)
        field? (string/starts-with? path (str name "."))
        field-from-path (if field? (string/replace path (re-pattern (str name ".")) "")
                                   nil)]
    (assoc elem :field? field? :field-name field-from-path)))

(defn apply-element [name]
  (comp
    add-cardinality
    #(add-field-name % name)))
