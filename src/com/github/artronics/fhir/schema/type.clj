(ns com.github.artronics.fhir.schema.type)

(defn fhir-type
  [element]
  (let [type (:type element)] "foo"))

(def fhir-path-primitives
  {"http://hl7.org/fhirpath/System.String"   :string
   "http://hl7.org/fhirpath/System.Boolean"  :boolean
   "http://hl7.org/fhirpath/System.Integer"  :integer
   "http://hl7.org/fhirpath/System.Decimal"  :decimal
   "http://hl7.org/fhirpath/System.DateTime" :dateTime
   "http://hl7.org/fhirpath/System.Time"     :time})

(def fhir-primitive-types
  #{:boolean
    :integer
    :string
    :decimal
    :uri
    :url
    :canonical
    :base64Binary
    :instant
    :date
    :dateTime
    :time
    :code
    :oid
    :id
    :markdown
    :unsignedInt
    :positiveInt
    :uuid})

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

