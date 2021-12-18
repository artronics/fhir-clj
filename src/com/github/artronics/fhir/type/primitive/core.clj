(ns com.github.artronics.fhir.type.primitive.core
  (:require [clojure.spec.alpha :as s]))

(s/def ::string string?)

(def fhir-path-primitives
  {"http://hl7.org/fhirpath/System.String"   :fhir.type.primitive/string
   "http://hl7.org/fhirpath/System.Boolean"  :fhir.type.primitive/boolean
   "http://hl7.org/fhirpath/System.Integer"  :fhir.type.primitive/integer
   "http://hl7.org/fhirpath/System.Decimal"  :fhir.type.primitive/decimal
   "http://hl7.org/fhirpath/System.DateTime" :fhir.type.primitive/dateTime
   "http://hl7.org/fhirpath/System.Time"     :fhir.type.primitive/time})

(def fhir-primitive-types
  #{:fhir.type.primitive/boolean
    :fhir.type.primitive/integer
    :fhir.type.primitive/string
    :fhir.type.primitive/decimal
    :fhir.type.primitive/uri
    :fhir.type.primitive/url
    :fhir.type.primitive/canonical
    :fhir.type.primitive/base64Binary
    :fhir.type.primitive/instant
    :fhir.type.primitive/date
    :fhir.type.primitive/dateTime
    :fhir.type.primitive/time
    :fhir.type.primitive/code
    :fhir.type.primitive/oid
    :fhir.type.primitive/id
    :fhir.type.primitive/markdown
    :fhir.type.primitive/unsignedInt
    :fhir.type.primitive/positiveInt
    :fhir.type.primitive/uuid})

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

(comment
  (def u "http://hl7.org/fhir/structuredefinition/structuredefinition-fhir-type")
  (def type1 {:extension [{:url u, :valueUrl "string"}]})
  (which-fhir-type type1))

(clojure.spec.alpha/valid? :fhir.type.primitive/positiveInt 3)
(. java.time.Instant now)
