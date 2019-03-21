(ns deercreeklabs.unit.plumatic-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]
   [deercreeklabs.baracus :as ba]
   [deercreeklabs.lancaster :as l]
   [deercreeklabs.lancaster.utils :as u]
   [deercreeklabs.unit.lancaster-test :as lt]
   [schema.core :as s :include-macros true])
  #?(:clj
     (:import
      (clojure.lang ExceptionInfo))))

(deftest test-plumatic-fixed-map
  (let [child-schema (l/fixed-map-schema ::child 2 l/boolean-schema)
        schema (l/array-schema child-schema)
        pschema (l/plumatic-schema schema)
        data [{(ba/byte-array [1 2]) true
               (ba/byte-array [3 4]) false}]]
    (is (nil? (s/check pschema data)))))

(deftest test-plumatic-primitives
  (is (= u/Nil (l/plumatic-schema l/null-schema)))
  (is (= s/Bool (l/plumatic-schema l/boolean-schema)))
  (is (= s/Int (l/plumatic-schema l/int-schema)))
  (is (= u/LongOrInt (l/plumatic-schema l/long-schema)))
  (is (= s/Num (l/plumatic-schema l/float-schema)))
  (is (= s/Num (l/plumatic-schema l/double-schema)))
  (is (= u/StringOrBytes (l/plumatic-schema l/string-schema)))
  (is (= u/StringOrBytes (l/plumatic-schema l/bytes-schema))))

(deftest test-plumatic-records
  (let [expected {s/Any s/Any
                  (s/required-key :add-to-cart-req/sku) s/Int
                  (s/required-key :add-to-cart-req/qty-requested) s/Int}
        _ (is (= expected (l/plumatic-schema lt/add-to-cart-req-schema)))
        expected {s/Any s/Any
                  :rec-w-array-and-enum/names [u/StringOrBytes]
                  :rec-w-array-and-enum/why (s/enum :why/all :why/stock
                                                    :why/limit)}]
    (is (= expected
           (l/plumatic-schema lt/rec-w-array-and-enum-schema)))))

(deftest test-plumatic-union
  (let [expected (s/conditional
                  int? s/Int
                  map? (l/plumatic-schema lt/add-to-cart-req-schema)
                  u/valid-bytes-or-string? u/StringOrBytes)]
    (is (= expected (l/plumatic-schema lt/union-schema)))))

(deftest test-plumatic-union-mult-rec
  (let [pl-sch (l/plumatic-schema lt/person-or-dog-schema)
        data #:person{:name "Apollo" :age 30}
        bad-data #:person{:name "Apollo" :age "not an integer"}]
    (is (= nil (s/check pl-sch data)))
    (is (not= nil (s/check pl-sch bad-data)))))

(deftest test-plumatic-maybe-missing-key
  (let [ps (l/plumatic-schema lt/rec-w-maybe-field-schema)
        data #:rec-w-maybe-field{:name "Boomer"}]
    (is (= nil (s/check ps data)))))

(deftest test-plumatic-maybe-nil-value
  (let [ps (l/plumatic-schema lt/rec-w-maybe-field-schema)
        data #:rec-w-maybe-field{:name "Boomer"
                                 :age nil}]
    (is (= nil (s/check ps data)))))