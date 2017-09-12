(ns deercreeklabs.avro-tools
  (:require
   [camel-snake-kebab.core :as csk]
   #?(:clj [cheshire.core :as json])
   [taoensso.timbre :as timbre :refer [debugf errorf infof]])
  #?(:cljs
     (:require-macros
      deercreeklabs.avro-tools)))

#?(:cljs
   (set! *warn-on-infer* true))

(defn type->default [avro-type]
  (case avro-type
    :null nil
    :boolean false
    :int (int -1)
    :long -1
    :float (float -1.0)
    :double (double -1.0)
    :bytes ""
    :string ""
    :array []
    :map {}
    :fixed ""))

(defn drop-schema-from-name [s]
  (-> (name s)
      (clojure.string/split #"-schema")
      (first)))

(defn make-named-schema
  [schema-ns schema-name]
  (let [avro-name (csk/->PascalCase (name schema-name))
        schema {:namespace nil ;; declare this now to preserve key order
                :name avro-name}]
    (if schema-ns
      (assoc schema :namespace (namespace-munge (name schema-ns)))
      (dissoc schema :namespace))))

(defn avro-rec
  ([schema-name fields]
   (avro-rec nil schema-name fields))
  ([schema-ns schema-name fields]
   (let [make-field (fn [[field-name field-type field-default]]
                      {:name (csk/->camelCase (name field-name))
                       :type field-type
                       :default (or field-default
                                    (type->default field-type))})]
     (-> (make-named-schema schema-ns schema-name)
         (assoc :type :record)
         (assoc :fields (mapv make-field fields))))))

(defn avro-rec-constructor [name* fields]
  (let [builder (-> (csk/->PascalCase name*)
                    (str "/newBuilder")
                    (symbol))]
    `(cond-> (~builder))))

(defn avro-enum
  ([schema-name symbols]
   (avro-enum nil schema-name symbols))
  ([schema-ns schema-name symbols]
   (let [make-enum-symbol (fn [sym]
                            (-> (name sym)
                                (csk/->SCREAMING_SNAKE_CASE)))]
     (-> (make-named-schema schema-ns schema-name)
         (assoc :type :enum)
         (assoc :symbols (mapv make-enum-symbol symbols))))))

(defn make-record-constructor-args [fields]
  (mapv #(symbol (name (first %))) fields))

(defmacro def-avro-named-schema
  [schema-fn schema-name args]
  (let [name* (drop-schema-from-name schema-name)
        args* (vec args)]
    `(def ~schema-name
       (let [ns# (.getName *ns*)]
         (~schema-fn ns# ~name* ~args*)))))

(defmacro def-avro-rec
  [schema-name & fields]
  `(def-avro-named-schema avro-rec ~schema-name ~fields))

;; (defmacro def-avro-enum
;;   [schema-name & symbols]
;;   `(def-avro-named-schema avro-enum ~schema-name ~symbols))

#(:clj
  (defn write-schema-file [filename schema]
    (spit filename (json/generate-string schema {:pretty true}))))
