(ns com.github.artronics.fhir.schema.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test.check.generators :as c]))

(s/def ::id
  (s/with-gen (s/and string? not-empty)
              #(gen/fmap (fn [id] (clojure.string/replace id #"[\.\" ]" ""))
                         (gen/not-empty (gen/string-ascii)))))

(s/def :fhir.schema.element/id ::id)
(s/def :fhir.schema.element/path ::id)
(s/def :fhir.schema.element/short string?)                  ;; Not sure if it's required

(def gen-id&path
  "Generates either and ::id or an {::id}.{::id}"
  (let [id (s/gen ::id)
        id-dot-id (gen/fmap (fn [[a b]] (str a "." b))
                            (gen/tuple id id))]
    (gen/one-of [id id-dot-id])))

(s/def :fhir.schema.element/id&path
  (s/with-gen (s/and
                (s/keys :req-un [:fhir.schema.element/id :fhir.schema.element/path])
                #(= (-> :id %) (-> :path %)))
              #(gen/bind gen-id&path
                         (fn [x] (gen/return {:id x :path x})))))

(s/def :fhir.schema/element-item
  (s/merge
    :fhir.schema.element/id&path
    (s/keys :req-un [:fhir.schema.element/short])))

(s/def :fhir.schema/element
  (s/coll-of :fhir.schema/element-item))

(s/def :fhir.schema/snapshot (s/keys :req-un [:fhir.schema/element]))

(s/def :fhir.schema/differential (s/keys :req-un [:fhir.schema/element]))

(def gen-element-with-common-id&path
  "Generate elements (one for snapshot and one differential) that (for some) shares :id and :path"
  (let [elem1 (s/gen :fhir.schema/element)
        elem2 (s/gen :fhir.schema/element)
        merge-id&path (fn [a b] (merge a (select-keys b [:id :path])))]
    (gen/fmap (fn [[e1 e2]]
                [e2
                 (map merge-id&path e1 e2)])
              (gen/tuple elem1 elem2))))

(def gen-snapshot&differential
  "Generates elements for both snapshot and differentials and then concatenates a list of elements to them
  that shares :id and :path"
  (let [snapshot (s/gen :fhir.schema/snapshot)
        differential (s/gen :fhir.schema/differential)
        common gen-element-with-common-id&path]
    (gen/fmap (fn [[snapshot differential [c-s c-d]]]
                (let [concat-element (fn [l c] (update l :element #(concat % c)))]
                  [(concat-element snapshot c-s)
                   (concat-element differential c-d)]))
              (gen/tuple snapshot differential common))))

(s/def :fhir.schema.resource/snapshot&differential
  (s/with-gen
    (s/keys :req-un [:fhir.schema/snapshot
                     :fhir.schema/differential])
    #(gen/bind gen-snapshot&differential
               (fn [[s d]] (gen/return {:snapshot s :differential d})))))

;; This contains everything in a resource except snapshot and differential
(s/def :fhir.schema.resource/base
  (s/keys :req-un [:fhir.schema.element/id]))

(s/def :fhir.schema/resource
  (s/merge :fhir.schema.resource/base
           :fhir.schema.resource/snapshot&differential))
