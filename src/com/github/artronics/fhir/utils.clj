(ns com.github.artronics.fhir.utils
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [com.github.artronics.fhir.schema.specs :refer :all])
  )

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

(defn add-field-name [elem]
  "It adds :field-name which is the :path minus the `id.` part. The elem is considered a field if path=`id.x.y.z`.
  Otherwise, :field-name will be nil."
  (let [path (:path elem)
        field-name (when (clojure.string/includes? path ".")
                     (clojure.string/replace-first path #"\S+?\." ""))]
    (assoc elem :field-name field-name)))

(defn- field-hierarchy
  [field, h, parent]
  (let [segment #(map keyword (clojure.string/split % #"\."))
        red-fn (fn [[h prev], n]
                 [(derive h n prev) n])]
    (reduce red-fn [h parent] (-> field :path segment))))

(defn fields-hierarchy
  [fields]
  (let [h (make-hierarchy)
        parent :fields-hierarchy
        red-fn (fn [acc field]
                 (first (field-hierarchy field acc parent)))]
    (reduce red-fn h fields)))

;; FIXME: It doesn't count for types where value is url like: http://hl7.org/fhirpath/System.String
(defn sanitise-path
  "It checks whether path is of form foo[x] and if yes it converts it to the form foo-Value1-Value2-...-ValueN
  where ValueN is the type of the field. For example medication[x] can be either medicationReference or medicationCodeableConcept"
  [element]
  (let [path (:path element)]
    (if (clojure.string/includes? path "[x]")
      (let [type (:type element)
            types (map (comp clojure.string/capitalize :code) type)
            type-comb (str "-" (clojure.string/join "-" types))]
        (clojure.string/replace path #"\[x\]" type-comb))
      path)))

(defn path->keyword [base path]
  (let [segs (clojure.string/split
               (if (empty? base) path (str base "." path))
               #"\.")
        name (last segs)
        ns (clojure.string/join "." (butlast segs))]
    (if (empty? ns)
      (keyword name)
      (keyword ns name))))

(comment
  (def elem {:path "med[x]" :type [{:code "Foo"} {:code "bar"}]})
  (sanitise-path elem)
  (namespace (path->keyword "kir.kos" "foo.bar.baz"))
  (path->keyword "kir" "foo.bar")
  (def fs [{:path "kir"} {:path "foo"} {:path "bar.kos"} {:path "foo.bar"} {:path "foo.baz"}])
  (fields-hierarchy fs))

(def elem->field
  (comp
    add-field-name))

