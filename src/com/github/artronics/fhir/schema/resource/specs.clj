(ns com.github.artronics.fhir.schema.resource.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))


(def res
  {:id     "MedicationRequest"
   :fields [{:field true :field-name "id"}]})

(def field
  ["id"])
(def exp-field :fhir.resource.4_0_1.MedicationRequest/id)

(s/def :fhir.resource.4_0_1.MedicationRequest/non-macro-id string?)

(defmacro resource-spec [field]
  `(s/def ~(keyword (str "fhir.resource.4_0_1.MedicationRequest/" field)) string?))

(comment
  (macroexpand '(resource-spec "id"))
  (resource-spec "id")
  (s/explain exp-field :fo)
  (s/explain :fhir.resource.4_0_1.MedicationRequest/non-macro-id :fo)
  (doc macroexpand))

