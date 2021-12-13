(ns com.github.artronics.fhir.schema.store
  (:require [datascript.core :as d]))

(def resource-schema
  {:url {:db/unique :db.unique/identity}})


(def conn (d/create-conn resource-schema))

(comment
  (d/reset-conn! conn (d/empty-db resource-schema))
  (d/transact! conn
               [{:url "foo" :resourceType "StructureDefinition" :kind "resource"}
                {:url "bar" :resourceType "StructureDefinition2" :kind "resource"}])
  (d/q '[:find ?v
         :where
         [?i :url "foo"]
         [?i :resourceType ?v]]
       @conn))


;;(let [conn (d/create-conn {:player {:db/unique :db.unique/identity}
;;                           :home {:db/valueType :db.type/ref}
;;                           :away {:db/valueType :db.type/ref}
;;                           :players {:db/unique :db.unique/identity
;;                                     :db/tupleAttrs [:home :away]}})])


