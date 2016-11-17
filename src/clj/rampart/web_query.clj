(ns rampart.web-query
  (:require [rampart.authorization :as auth]))

(defn- gather-params [req]
  (merge
   (:params req)              ;; accomodates standard query params
   (-> req :params :filter))) ;; accomodates json-api's putting query params into filters

(defn make-query [subsystem-name query-name request]
  (let [customer-id (auth/request->customer-id request)]
    {:subsystem   subsystem-name
     :query-name  query-name
     :params      (gather-params request)
     :customer-id customer-id
     }))

(defn make-query-request [subsystem-name query-name request]
  (let [query       (make-query subsystem-name query-name request)
        server      (-> request :params :env :server)
        customer-id (:customer-id query)]
    {:request request
     :query
     (cond-> query
       server      (assoc-in [:query :server] server)
       customer-id (assoc-in [:query :customer-id] customer-id)
       )}))
  ;; (let [query       {:request request
  ;;                    :query   (make-query subsystem-name query-name request)
  ;;                    }
  ;;       server      (-> request :params :env :server)
  ;;       customer-id (:customer-id query)]
  ;;   (cond-> query
  ;;     server      (assoc-in [:query :server] server)
  ;;     customer-id (assoc-in [:query :customer-id] customer-id)
  ;;     )))

