(ns spacon.components.store.db
  (:require [spacon.db.conn :as db]
            [spacon.util.db :as dbutil]
            [yesql.core :refer [defqueries]]
            [spacon.specs.store]
            [clojure.data.json :as json]
            [clojure.spec :as s]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [spacon.entity.store :refer :all]))

;; define sql queries as functions
(defqueries "sql/store.sql" {:connection db/db-spec})

(def uuid-regex #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

(defn- sanitize [store]
  (transform-keys ->kebab-case-keyword (dissoc store :created_at :updated_at :deleted_at)))

(defn map->entity [t]
  (-> t
      (cond-> (nil? (:options t)) (assoc :options nil))
      (cond-> (some? (:options t)) (assoc :options
                                          (json/write-str (:options t))))
      (assoc :default_layers (dbutil/->StringArray (:default-layers t)))))

(defn row-fn [row]
  (-> row
      (dbutil/sqlarray->vec :default_layers)
      (dbutil/sqluuid->str :id)))

(def result->map
  {:result-set-fn doall
   :row-fn row-fn
   :identifiers clojure.string/lower-case})

(defn all
  "Lists all the active stores"
  []
  (let [res (store-list-query {} result->map)]
    (map sanitize res)))

(defn find-by-id
  "Gets store by store identifier"
  [id]
  (if (re-matches uuid-regex id)
    (some-> (find-by-id-query {:id (java.util.UUID/fromString id)} result->map)
            (first)
            (sanitize))
    nil))

(defn create
  "Creates a store"
  [t]
  (let [entity (map->entity t)
        tr (transform-keys ->snake_case_keyword entity)]
    (if-let [new-store (insert-store<! tr)]
      (sanitize (assoc t :id (.toString (:id new-store))))
      nil)))

(defn modify
  "Update a data store"
  [id t]
  (let [entity (map->entity (assoc t :id (java.util.UUID/fromString id)))
        tr (transform-keys ->snake_case_keyword entity)
        updated-store (update-store<! tr)]
    (sanitize (row-fn t))))

(defn delete
  "Deactivates a store"
  [id]
  (delete-store! {:id (java.util.UUID/fromString id)}))

(s/fdef all
        :args empty?
        :ret (s/coll-of :spacon.specs.store/store-spec))

(s/fdef create
        :args (s/cat :t :spacon.specs.store/store-spec)
        :ret (s/spec :spacon.specs.store/store-spec))