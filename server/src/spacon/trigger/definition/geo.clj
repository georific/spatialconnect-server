(ns spacon.trigger.definition.geo
  (:require [spacon.trigger.protocol :as proto]
            [cljts.io :as jtsio]
            [cljts.relation :as spatial-relation]
            [clojure.data.json :as json]))

(defn rhs->jtsgeom
  "Takes a clojure map of the geojson and returns it as 
  a JTS default feature collection"
  [c]
  (jtsio/read-feature-collection
   (json/write-str c)))

(defn check-simple-feature
  "Returns true if point is in feature"
  [point feature]
  (spatial-relation/within? point feature))

(defn check-feature-collection
  "Returns true if point is within any geometry in feature-collection"
  [point feature-collection]
  (if-let [features (.features feature-collection)]
    (if (.hasNext features)
      (loop [feature (-> features .next .getDefaultGeometry)]
        (if (check-simple-feature point feature)
          true
          (if (.hasNext features)
            (recur (-> features .next .getDefaultGeometry))
            false))); fails within, false
      false) ; has no .next, false
    false)); has no features, false

(def clause-case-map {org.geotools.feature.DefaultFeatureCollection check-feature-collection,
                      org.geotools.feature.simple.SimpleFeatureImpl check-simple-feature})

(defrecord WithinClause [trigger-id clause]
  proto/IClause
  (field-path [this] (:clause this))
  (predicate [this] :$geowithin)
  (check [this value]
    (if-let [f (clause-case-map (type (:clause this)))]
      (apply f [value (:clause this)])
      false))
  (notification [this test-value]
    (str (cljts.io/write-geojson test-value) " was within.")))

(defn make-within-clause
  "Takes a right hand side clause of a rule and returns a jts geometry"
  [id clause]
  (->WithinClause id (rhs->jtsgeom clause)))

