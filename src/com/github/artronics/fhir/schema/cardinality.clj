(ns com.github.artronics.fhir.schema.cardinality)

(defn cardinality [element]
  "Cardinality based on {:base {:min x :max y} :min a :max b}. Whether base's max: " * " should be interpreted as array or not
  is up to interpretation. Refer to the table here: https://build.fhir.org/profiling.html#cardinality"
  (let [parse #(if (= String (type %))
                 (if (= "*" %) -1 (Integer/parseInt %))
                 %)
        base-min (:min (:base element))
        base-max (:max (:base element))
        derived-min (:min element)
        derived-max (:max element)]
    (case (map parse [base-min base-max derived-min derived-max])
      [0 1, 0 0] :not-used
      [0 1, 0 1] :optional
      [0 1, 1 1] :required
      ;; Similar to above but the base has `max: "*"` which means it CAN BE represented as an array
      [0 -1, 0 0] :emtpy
      [0 -1, 0 1] :at-most-1
      [0 -1, 0 -1] :optional-many
      [0 -1, 1 1] :only-1
      [0 -1, 1 -1] :at-least-1

      [1 1, 1 1] :required

      [1 -1, 1 1] :only-1
      [1 -1, 1 -1] :at-least-1
      ;; TODO: This only covers 0, 1 and "*" cases. We need to cover arbitrarily numbers e.x. :only-42
      ;; FIXME: What is the default value if we can't figure out cardinality? Currently :optional
      :optional)))

