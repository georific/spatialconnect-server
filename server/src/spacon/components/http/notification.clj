(ns spacon.components.http.notification
  (:require [spacon.components.http.response :as response]
            [spacon.components.http.intercept :as intercept]
            [clojure.tools.logging :as log]
            [spacon.components.notification.core :as notifapi]))

(defn http-get-notif [notif-comp context]
  (let [id (Integer/parseInt (get-in context [:path-params :id]))]
    (response/ok (notifapi/find-notif-by-id notif-comp id))))

(defn http-send-notif
  "Send push notification json body"
  [notif-comp request]
  (let [notif-data (get-in request [:json-params])]
    (notifapi/send->devices notif-data)
    (response/ok "notification sent")))


(defn routes [notif-comp]
  #{["/api/notifications/:id" :get (conj intercept/common-interceptors (partial http-get-notif notif-comp)) :route-name :get-notif]
    ["/api/notifications" :post (conj intercept/common-interceptors (partial http-send-notif notif-comp)) :route-name :send-notif]})

