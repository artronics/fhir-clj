(ns com.github.artronics.fhir.core
  (:require [clojure.string :as string]))

(defn create-fields [sn df]
  (let [eq-id? (fn [a b] (= (:id a) (:id b)))
        cons-if-not-nil #(if %1 (cons %1 %2) %2)]
    (loop [fields [], sn sn, df df]
      (let [sn0 (first sn)
            sn-x (rest sn)
            apply-diff (fn [df-i] (when (eq-id? sn0 df-i) [(merge sn0 df-i), df-i]))
            [fields df] (if-let [[field consumed-df] (some apply-diff df)]
                          [(cons field fields) (remove #(= % consumed-df) df)]
                          [(cons-if-not-nil sn0 fields) df])]
        (if (seq sn-x)
          (if (seq df)
            (recur fields sn-x df)
            (concat fields sn-x))
          (concat fields df))))))

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

