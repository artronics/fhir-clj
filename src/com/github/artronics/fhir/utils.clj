(ns com.github.artronics.fhir.utils
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [com.github.artronics.fhir.schema.specs :refer :all]))

(defn- eq-id? [a b] (= (:id a) (:id b)))

(defn- apply-diff [differential snapshots]
  "Find matching id in snapshots and merge the matching differential to it.
  Returns [merged , snapshots minus matched] or
  identity if no match was found"
  (let [matched-sn? (fn [sn-i] (when (eq-id? differential sn-i) sn-i))
        matched-sn (some matched-sn? snapshots)]
    (if matched-sn
      [(merge matched-sn differential) (remove #(= % matched-sn) snapshots)]
      [differential snapshots])))

(defn- apply-diff-reducer-fn
  [[acc sn-x] df-i]
  (let [[merged sn-x] (apply-diff df-i sn-x)]
    [(conj acc merged) sn-x]))

;; TODO: Should we consider merging lists as well? Currently, when we merge the lists will be substituted completely.
(defn merge-elements [sn df]
  (let [[acc sn-x] (reduce apply-diff-reducer-fn [[] sn] df)]
    ;; concat whatever is left from snapshots i.e. the one that didn't match with any id in differentials.
    (concat acc sn-x)))

(defn- check-returns [rets sn df]
  (let [sc (count sn)
        dc (count df)
        rc (count rets)]
    (and (<= (max sc dc) rc (+ sc dc))
         (let [check-merge (fn [ret-i]
                             (let [find-eq (fn [nx] (first (filter #(eq-id? % ret-i) nx)))
                                   matched-sn (find-eq sn)
                                   matched-df (find-eq df)]
                               (= ret-i (merge matched-sn matched-df))))]
           (every? check-merge rets)))))

(s/fdef merge-elements
        :args (s/cat :snapshot :fhir.schema/element
                     :differentials :fhir.schema/element)
        :ret :fhir.schema/element
        :fn #(let [ret (-> % :ret)
                   sn (-> % :args :snapshot)
                   df (-> % :args :differentials)]
               (check-returns ret sn df)))

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
      ;; Similar to above but the base has max: many which means it CAN BE represented as an array
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

(defn add-cardinality [elem]
  (assoc elem :cardinality (cardinality elem)))

(defn add-field-name [elem]
  "It adds :field-name which is the :path minus the `id.` part. the elem must be the one with path=`id.x.y.z`. i.e.
  exclude the first element in :element and the rest can be considered as a field."
  (let [path (:path elem)
        field-name (clojure.string/replace-first path #"\S+?\." "")]
    (assoc elem :field-name field-name)))

(defn apply-element [name]
  (comp
    add-cardinality
    #(add-field-name %)))
