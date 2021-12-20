(ns com.github.artronics.fhir.type.value-set
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as json]))

(def appstatus
  (-> "test/schema/CodeSystem-appointmentstatus.json"
      io/resource
      io/file
      slurp
      (json/read-str :key-fn keyword)))

(def base-ns "fhir.type.code-system")

(defn- code->keyword2 [code-sys-name code]
  (let [ns (str base-ns "." code-sys-name)]
    (keyword ns code)))

(defn- code->keyword [code-sys-id]
  (keyword base-ns code-sys-id))

(defn make-code-system [code-sys-resource]
  (let [id (:id code-sys-resource)
        concepts (:concept code-sys-resource)
        codes (set (map :code concepts))
        type-name (code->keyword id)]
    {type-name codes}))

(defn make-validator2 [code-system code-id]
  (let [id (code->keyword code-id)
        enums (id code-system)]
    (fn [enum] (enums enum))))

(comment
  (def appstatus-cs (make-code-system appstatus))
  (def check (make-validator appstatus-cs (:id appstatus)))
  (s/valid? check "arrived")
  (code->keyword "appstatus")
  (println appstatus))
