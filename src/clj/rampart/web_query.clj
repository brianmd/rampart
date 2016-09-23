(ns rampart.web-query
  (:require [clojure.string :as str]

            [rampart.authorization :as auth]
            [summit.utils.core :as utils]))

(defn- gather-params [req]
  (merge
   (:params req)               ;; to accomodate standard query params
   (:filter (:params req))))   ;; to accomodate json-api's putting query params into filters

(defn make-query [subsystem-name query-name request]
  (let [customer-id (auth/request->customer-id request)]
    {:subsystem   subsystem-name
     ;; :service-name???? 3
     :query-name  query-name
     :params      (gather-params request)
     ;; :customer-id (or customer-id 2742)
     :customer-id customer-id
     }))

(defn make-query-request [subsystem-name query-name request]
  (let [server      (-> request :params :env :server)
        query       {:request request
                     :query   (make-query subsystem-name query-name request)
                     }
        customer-id (:customer-id query)]
    (cond-> query
      ;; true        (assoc :serv server)
      server      (assoc-in [:query :server] server)
      customer-id (assoc-in [:query :customer-id] customer-id)
      )))

