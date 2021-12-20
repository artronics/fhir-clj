(ns com.github.artronics.fhir.type.spec.primitive
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [com.github.artronics.fhir.type.spec.generator :refer :all]))

;; FIXME: Technically this should be unicode not just ASCII
(s/def :fhir.type.primitive/string string?)
(s/def :fhir.type.primitive/boolean boolean?)
(s/def :fhir.type.primitive/integer int?)
(s/def :fhir.type.primitive/decimal double?)

(s/def :fhir.type.primitive/markdown string?)

(s/def :fhir.type.primitive/uri string?)
(s/def :fhir.type.primitive/url
  (s/with-gen string?
              #(url-gen)))
(s/def :fhir.type.primitive/canonical string?)

(s/def :fhir.type.primitive/unsignedInt
  (s/with-gen (s/and int? pos-int? (comp not zero?))
              #(gen/fmap (fn [n] (Math/abs ^int n)) (gen/int))))

(s/def :fhir.type.primitive/positiveInt
  (s/with-gen (s/and int? pos-int?)
              #(gen/fmap (fn [n] (inc (Math/abs ^int n))) (gen/int))))

(s/def :fhir.type.primitive/uuid
  (s/with-gen (s/and string? #(re-matches uuid-re %))
              #(uuid-gen)))

(s/def :fhir.type.primitive/oid
  (s/with-gen (s/and string? #(re-matches oid-re %))
              #(oid-gen)))

(s/def :fhir.type.primitive/time
  (s/with-gen (s/and string? #(re-matches time-re %))
              #(time-gen)))

(s/def :fhir.type.primitive/date
  (s/with-gen (s/and string? #(re-matches date-re %))
              #(date-gen)))

(s/def :fhir.type.primitive/dateTime
  (s/with-gen (s/and string? #(re-matches date-time-re %))
              #(date-time-gen)))

(s/def :fhir.type.primitive/instant
  (s/with-gen (s/and string? #(re-matches instant-re %))
              #(instant-gen)))
