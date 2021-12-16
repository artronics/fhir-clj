(ns com.github.artronics.fhir.generators
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.coerce :as c])

  (:import (java.nio ByteBuffer)
           (java.util UUID Base64)))

(def time-re #"([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\.[0-9]+)?")
(def date-time-re #"([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])(T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\.[0-9]+)?(Z|(\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?")
(def date-re #"([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1]))?)?")
(def instant-re #"([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\.[0-9]+)?(Z|(\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))")
(def oid-re #"urn:oid:[0-2](\.(0|[1-9][0-9]*))+")

(defn- pad-zero [d n]
  "Pad number d with zeros until length is n. Example d=42 n=4 -> 0042"
  (let [num (str d)
        pad (- n (count num))
        zeros (apply str (repeat pad "0"))]
    (str zeros num)))

(defn- abs [n] (Math/abs ^int n))

(defn- pos-int-gen
  ([] (gen/fmap #(abs %) (gen/int)))
  ([min max] (gen/fmap #(abs %) (gen/choose min max)))
  ([max] (pos-int-gen 0 max)))

(defn- pos-int-str-gen
  ([min max] (gen/fmap #(pad-zero % (count (str max))) (pos-int-gen min max)))
  ([max] (pos-int-str-gen 0 max)))

(defn b64-uuid
  "Generates a new UUID as Base64."
  []
  (let [uuid-bytes (byte-array 16)
        buffer (ByteBuffer/wrap uuid-bytes)
        uuid (UUID/randomUUID)
        encoder (Base64/getEncoder)]
    (.putLong buffer (.getMostSignificantBits uuid))
    (.putLong buffer (.getLeastSignificantBits uuid))
    (.encodeToString encoder uuid-bytes)))

(defn unique-id []
  "Generates unique identifiers. It's based on base64 uuid for uniqueness and replace non-alphanumerics
  for a uppercase character to make it a valid id"
  (let [rand-char #(char (+ 65 (rand-int 26)))
        char->char (fn [s] (clojure.string/replace s #"[=/\+\-_]" (str (rand-char))))]
    (str (rand-char) (char->char (b64-uuid)))))

(defn time-zone-gen
  ([format]
   (let [hh (pos-int-str-gen 12)
         mm #(gen/fmap (fn [n] (nth ["00" "15" "30" "45"] n)) (gen/choose 0 3))
         p-or-m #(rand-nth ["+" "-"])]
     (cond
       (= :z format) (gen/return "Z")
       (= :offset) (gen/bind hh #(gen/fmap (fn [m] (str (p-or-m) % ":" m)) (mm))))))
  ([] (gen/one-of [(time-zone-gen :z) (time-zone-gen :offset)])))

(defn time-gen
  ([format]
   (let [hh (pos-int-str-gen 23)
         mm (pos-int-str-gen 59)
         ss (pos-int-str-gen 60)
         tz (time-zone-gen)
         make (fn [[hh mm ss]] (str hh ":" mm ":" ss))
         milli (pos-int-str-gen 999)]
     (cond
       (= :simple format) (gen/fmap make (gen/tuple hh mm ss))
       (= :sss format) (gen/bind (time-gen :simple) #(gen/fmap (fn [t] (str % "." t)) milli))
       (= :sss-timezone format) (gen/bind (time-gen :sss) #(gen/fmap (fn [t] (str % t)) tz))
       (= :timezone format) (gen/bind (time-gen :simple) #(gen/fmap (fn [z] (str % z)) tz)))))
  ([] (gen/one-of
        [(time-gen :simple)
         (time-gen :sss)
         (time-gen :sss-timezone)
         (time-gen :timezone)])))

(defn date-gen
  ([format]
   (let [y (pos-int-str-gen 1990 2050)
         m (pos-int-str-gen 1 12)
         d (pos-int-str-gen 1 31)]
     (cond
       (= format :yyyy) y
       (= format :yyyy-mm) (gen/bind y #(gen/fmap (fn [mm] (str % "-" mm)) m))
       (= format :yyyy-mm-dd) (gen/bind (date-gen :yyyy-mm) #(gen/fmap (fn [dd] (str % "-" dd)) d)))))
  ([]
   (gen/one-of
     [(date-gen :yyyy)
      (date-gen :yyyy-mm)
      (date-gen :yyyy-mm-dd)])))

(defn- rand-instant []
  (let [now (c/to-long (t/today))
        r-inst (long (rand now))]
    (c/from-long r-inst)))

(defn instant-gen []
  (let [gen #(tf/unparse (tf/formatter %) (rand-instant))
        ;; FIXME: This doesn't produce offset.
        gen-tz #(tf/unparse (tf/formatter %) (t/to-time-zone (rand-instant) (t/time-zone-for-offset 2)))]
    (gen/one-of
      [(gen/return (gen :date-time))
       (gen/return (gen :date-time-no-ms))])))

(defn date-time-gen
  ([format]
   (cond
     (= :date format) (date-gen)
     (= :date-time format) (gen/bind (date-gen :yyyy-mm-dd) #(gen/fmap (fn [t] (str % "T" t)) (time-gen)))))
  ([] (gen/one-of
        [(date-time-gen :date) (date-time-gen :date-time)])))

(defn- interpose-gen [gen s]
  (gen/fmap #(apply str (interpose s %)) gen))

(defn oid-gen []
  "urn:oid:{n}.{{m0}.{m1}...{mN}} for N <= 9 and 0<= n <=2"
  (let [n (gen/choose 0 2)
        m (pos-int-gen)
        ms (gen/vector m 1 9)
        ms-str (interpose-gen ms ".")]
    (gen/bind n
              #(gen/fmap (fn [mi] (str "urn:oid:" % "." mi)) ms-str))))

(defn- alpha-numerics-gen
  ([min max] (gen/not-empty (gen/vector (gen/not-empty (gen/string-alphanumeric)) min max)))
  ([max] (alpha-numerics-gen 0 max)))

(defn url-gen []
  (let [protocols ["http" "https" "ftp" "ftps" "mailto" "mllp"]
        protocol (gen/fmap #(nth protocols %) (pos-int-gen (-> protocols count dec)))
        domains (interpose-gen (alpha-numerics-gen 1 20) ".")
        top-domain (gen/not-empty (gen/string-alphanumeric 1 5))
        path (interpose-gen (gen/one-of [(gen/return []) (alpha-numerics-gen 20)]) "/")
        all (gen/tuple protocol domains top-domain path)]
    (gen/fmap (fn [[p d td pt]] (str p "://" d "." td "/" pt)) all)))
