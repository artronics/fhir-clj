(ns com.github.artronics.fhir.utils
  (:require [clojure.string :as string]))

(defn- eq-id? [a b] (= (:id a) (:id b)))

(defn- apply-diff [snapshot differentials]
  "Find matching id in differentials and merge it to snapshot.
  Returns [merged , differentials minus matched] or
  identity if no match was found"
  (let [matched-df? (fn [df-i] (when (eq-id? snapshot df-i) df-i))
        matched-df (some matched-df? differentials)]
    (if matched-df
      [(merge snapshot matched-df) (remove #(= % matched-df) differentials)]
      [snapshot differentials])))

(defn- apply-diff-reducer-fn
  [[acc df-x] sn-i]
  (let [[merged df-x] (apply-diff sn-i df-x)]
    [(conj acc merged) df-x]))

(defn merge-elements [sn df]
  (let [[acc df-x] (reduce apply-diff-reducer-fn [[] df] sn)]
    ;; concat whatever is left from differentials i.e. the one that didn't match with any id in snapshots
    (concat acc df-x)))

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
