(ns com.github.artronics.fhir.utils
  (:require [clojure.string :as string]))

(defn eq-id? [a b] (= (:id a) (:id b)))

(defn apply-diff [snapshot differentials]
  (let [matched-df? (fn [df-i] (when (eq-id? snapshot df-i) df-i))
        matched-df (some matched-df? differentials)]
    (if matched-df
      [(merge snapshot matched-df) (remove #(= % matched-df) differentials)]
      [snapshot differentials])))

(defn apply-diff-reducer
  [[acc df-x] sn-i]
  (let [[merged df-x] (apply-diff sn-i df-x)]
    [(conj acc merged) df-x]))

(defn merge-elements3 [sn df]
  (let [[acc df-x] (reduce apply-diff-reducer [[] df] sn)]
    ;; concat whatever is left from differentials i.e. the one that didn't match with any id in snapshots
    (concat acc df-x)))

(defn merge-elements2 [sn df]
  (let [conj-nil #(if %2 (conj %1 %2) %1)]
    (loop [elements [], sn sn, df df]
      (let [sn0 (first sn)
            sn-x (rest sn)
            [merged df-x] (apply-diff sn0 df)
            elements (conj-nil elements merged)]
        (if (seq sn-x)
          (if (seq df-x)
            (recur elements sn-x df-x)
            (concat elements sn-x))
          (concat elements df-x))))))

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
