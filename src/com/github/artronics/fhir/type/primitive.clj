(ns com.github.artronics.fhir.type.primitive
  (:require [clojure.spec.alpha :as s]
            [com.github.artronics.fhir.type.spec.primitive :as p]))

(def fhir-path-primitives
  {"http://hl7.org/fhirpath/System.String"   ::string
   "http://hl7.org/fhirpath/System.Boolean"  ::boolean
   "http://hl7.org/fhirpath/System.Integer"  ::integer
   "http://hl7.org/fhirpath/System.Decimal"  ::decimal
   "http://hl7.org/fhirpath/System.DateTime" ::dateTime
   "http://hl7.org/fhirpath/System.Time"     ::time})

(def fhir-primitive-types
  #{::boolean
    ::integer
    ::string
    ::decimal
    ::uri
    ::url
    ::canonical
    ::base64Binary
    ::instant
    ::date
    ::dateTime
    ::time
    ::code
    ::oid
    ::id
    ::markdown
    ::unsignedInt
    ::positiveInt
    ::uuid})

(defn i-compare [a b]
  (= (clojure.string/lower-case a) (clojure.string/lower-case b)))

(def ftype-url
  (clojure.string/lower-case "http://hl7.org/fhir/StructureDefinition/structuredefinition-fhir-type"))
(defn ftype-url? [url]
  (i-compare ftype-url url))

(defn which-fhir-type [element]
  (let [extensions (-> element :type :extension)
        type (filter #(ftype-url? (:url %)) extensions)]
    ;; ElementDefinition, eld-13, Types must be unique by code
    ;; FIXME: handle multiple types/codes
    (when-let [ext (first type)]
      (-> :valueUrl ext keyword fhir-primitive-types))))
