(ns deercreeklabs.unit.sub-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [deercreeklabs.baracus :as ba]
   [deercreeklabs.lancaster :as l]
   [deercreeklabs.lancaster.utils :as u]
   [deercreeklabs.unit.lancaster-test :as lt]
   [schema.core :as s :include-macros true])
  #?(:clj
     (:import
      (clojure.lang ExceptionInfo))))

(l/def-record-schema user-schema
  [:name l/string-schema]
  [:nickname l/string-schema])

(l/def-record-schema msg-schema
  [:user user-schema]
  [:text l/string-schema])

(l/def-record-schema sys-state-schema
  [:msgs (l/array-schema msg-schema)]
  [:users (l/array-schema user-schema)])

(l/def-record-schema foo-schema
  [:foo/a :required l/int-schema])

(l/def-record-schema bar-schema
  [:bar/a :required l/string-schema])

(l/def-record-schema foo-foos-schema
  [:foo foo-schema]
  [:foos (l/map-schema foo-schema)])

(l/def-map-schema map-of-fbs-schema
  (l/union-schema [foo-schema bar-schema]))

(l/def-record-schema person-schema
  [:name l/string-schema]
  [:age l/int-schema]
  [:children (l/array-schema ::person)])

(def mary
  {:name "Mary"
   :age 11
   :children []})

(def bob
  {:name "Bob"
   :age 10
   :children []})

(def ralph
  {:name "Ralph"
   :age 100
   :children [bob mary]})

(l/def-record-schema tree-schema
  [:value l/int-schema]
  [:right ::tree]
  [:left ::tree])

(deftest test-schema-at-path-nested-recs
  (let [path [:req :sku]
        ret (-> (l/schema-at-path lt/add-to-cart-rsp-schema path)
                (u/edn-schema))]
    (is (= :int ret))))

(deftest test-schema-at-path-rec
  (let [path [:req]
        ret (-> (l/schema-at-path lt/add-to-cart-rsp-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :deercreeklabs.unit.lancaster-test/add-to-cart-req ret))))

(deftest test-schema-at-path-rec-root
  (let [path []
        ret (-> (l/schema-at-path lt/add-to-cart-rsp-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :deercreeklabs.unit.lancaster-test/add-to-cart-rsp ret))))

(deftest test-schema-at-path-array-in-rec-1
  (let [path [:names]
        ret (-> (l/schema-at-path lt/rec-w-array-and-enum-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :array ret))))

(deftest test-schema-at-path-array-in-rec-2
  (let [path [:names 1234]
        ret (-> (l/schema-at-path lt/rec-w-array-and-enum-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :string ret))))

(deftest test-schema-at-path-enum-in-rec
  (let [path [:why]
        ret (-> (l/schema-at-path lt/rec-w-array-and-enum-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :union ret))))

(deftest test-schema-at-path-map-in-rec-1
  (let [path [:name-to-age]
        ret (-> (l/schema-at-path lt/rec-w-map-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :union ret))))

(deftest test-schema-at-path-map-in-rec-2
  (let [path [:name-to-age "Bo"]
        ret (-> (l/schema-at-path lt/rec-w-map-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :int ret))))

(deftest test-schema-at-path-union-root
  (let [path []
        ret (-> (l/schema-at-path map-of-fbs-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :map ret))))

(deftest test-schema-at-path-union-one-arg
  (let [path ["a"]
        ret (-> (l/schema-at-path map-of-fbs-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :union ret))))

(deftest test-schema-at-path-union-two-args-int
  (let [path ["a" :foo/a]
        ret (-> (l/schema-at-path map-of-fbs-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :int ret))))

(deftest test-schema-at-path-union-two-args-str
  (let [path ["a" :bar/a]
        ret (-> (l/schema-at-path map-of-fbs-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :string ret))))

(deftest test-schema-at-path-repeated-schema
  (let [path [:foos "x"]
        ret (-> (l/schema-at-path foo-foos-schema path)
                (u/edn-schema)
                (u/edn-schema->name-kw))]
    (is (= :deercreeklabs.unit.sub-test/foo ret))))

(deftest test-schema-at-path-nil-path
  (let [ret (-> (l/schema-at-path foo-foos-schema nil)
                (l/edn))]
    (is (= (l/edn foo-foos-schema) ret))))

(deftest test-schema-at-path-recursive
  (let [schema (l/schema-at-path person-schema [:children])]
    (is (lt/round-trip? schema [ralph]))))

(deftest test-schema-at-path-recursive-deep
  (let [schema (l/schema-at-path person-schema
                                 [:children 0 :children 1 :children])]
    (is (lt/round-trip? schema [ralph]))))

(l/def-record-schema z-schema
  [:z l/string-schema])

(def d-schema
  (l/array-schema z-schema))

(l/def-record-schema a-schema
  [:aa d-schema])

(l/def-record-schema b-schema
  [:bb d-schema])

(l/def-record-schema root-schema
  [:a a-schema]
  [:b b-schema]
  [:c b-schema])

(def c
  {:bb [{:z "test"} {:z "another"}]})

(deftest test-schema-at-path-name-expansion-deeper-than-path-nesting
  (let [schema (l/schema-at-path root-schema [:c])]
    (is (lt/round-trip? schema c))))

(deftest test-schema-at-path-evolution
  (is (nil? (l/schema-at-path sys-state-schema [:new-state-field])))
  (is (nil? (l/schema-at-path sys-state-schema [:msgs 0 :new-msg-field]))))

(deftest test-member-schemas-recursive
  (let [field-schema (l/schema-at-path tree-schema [:right])
        members (l/member-schemas field-schema)
        _ (is (= 2 (count members)))
        _ (is (= [:null :record] (map l/schema-type members)))
        tree-schema (second members)
        tree {:right {:right {:value 7}}
              :left {:left {:value 8}}
              :value 3}
        encoded (l/serialize tree-schema tree)
        decoded (l/deserialize-same tree-schema encoded)]
    (is (= tree decoded))))

(deftest test-member-schema-at-branch-recursive
  (let [field-schema (l/schema-at-path tree-schema [:right])
        tree-schema (l/member-schema-at-branch field-schema 1)
        tree {:right {:right {:value 7}}
              :left {:left {:value 8}}
              :value 3}
        encoded (l/serialize tree-schema tree)
        decoded (l/deserialize-same tree-schema encoded)]
    (is (= tree decoded))))

(l/def-enum-schema the-enum-schema :a :b)

(l/def-record-schema record-enum-schema
  [:choice the-enum-schema])

(l/def-record-schema enclosing-schema
  [:enclosed record-enum-schema])

(l/def-record-schema wrapping-record-schema
  [:wrapped enclosing-schema])

(l/def-map-schema wrapping-map-schema enclosing-schema)

(deftest test-path-navigation
  ;; You may choose to have l/schema-a-path do the path navigation.
  (is (lt/round-trip?
       (l/schema-at-path wrapping-record-schema [:wrapped :enclosed])
       {:choice :a}))
  ;; Or you may choose to do it yourself this way,
  (is (lt/round-trip?
       (-> (l/schema-at-path wrapping-record-schema [:wrapped])
           (l/schema-at-path [:enclosed]))
       {:choice :a}))
  ;; or this way.
  (is (lt/round-trip?
       (-> (u/child-schema wrapping-record-schema :wrapped)
           (u/child-schema :enclosed))
       {:choice :a}))
  ;; Or if you happen to know the union branches and are under the hood.
  (is (lt/round-trip?
       (-> (u/child-schema wrapping-record-schema :wrapped)
           (u/child-schema 1)
           (u/child-schema :enclosed))
       {:choice :a}))
  ;; And with maps as well
  (is (lt/round-trip?
       (l/schema-at-path wrapping-map-schema ["wrapped" :enclosed])
       {:choice :a}))
  (is (lt/round-trip?
       (-> (l/schema-at-path wrapping-map-schema ["wrapped"])
           (l/schema-at-path [:enclosed]))
       {:choice :a}))
  (is (lt/round-trip?
       (-> (u/child-schema wrapping-map-schema)
           (u/child-schema :enclosed))
       {:choice :a})))

(deftest test-union-path-navigation
  ;; schema-at-path doesn't support union branch navigation by default.
  (is (thrown-with-msg?
       #?(:clj ExceptionInfo :cljs js/Error)
       #"No matching schema in union for key `1`"
       (lt/round-trip?
        (l/schema-at-path wrapping-record-schema [:wrapped 1 :enclosed])
        {:choice :a})))
  ;; instead it assumes you want to find the matching schema in a union and it
  ;; does so.
  (is (lt/round-trip?
       (l/schema-at-path wrapping-record-schema [:wrapped :enclosed])
       {:choice :a}))
  ;; but you can have it assume that when you have a union schema and an
  ;; integer in the corresponding path index you mean for it to take it as a
  ;; union branch
  (is (lt/round-trip?
       (l/schema-at-path wrapping-record-schema
                         [:wrapped 1 :enclosed]
                         {:branches? true})
       {:choice :a})))
