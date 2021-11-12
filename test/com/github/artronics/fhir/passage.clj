(ns com.github.artronics.fhir.passage
  (:require [clojure.test :refer :all]))

;;           A [a, b]
;;          / \
;;  [c, d] X   Y [e, f]
[:A [:X :Y]]
[:A [:a :b] :X [:c :d] :Y [:e :f]]

[:A [:a :b] [:X [:c :d] []] [:Y [:e :f] []]]
;;
;;

(def data-a {:snapshot {:element
                        [{:path "A.a" :base {:path "A.a"}} {:path "A.b" :base {:path "A.b"}}]}})
(def data-x {:snapshot {:element
                        [{:path "X.c" :base {:path "X.c"}} {:path "X.d" :base {:path "X.d"}}
                         {:path "X.a" :base {:path "A.a"}} {:path "X.b" :base {:path "A.b"}}]}})
(def exp [:Resource
          [[:meta]
           [:Patient
            [:name]]]])

(def d-in {:element [{:id "Patient.name"} {:id "Resource.meta"}]})
