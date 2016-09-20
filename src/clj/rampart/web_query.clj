(ns rampart.web-query
  (:require [clojure.string :as str]

            [rampart.authorization :as auth]
            [summit.utils.core :as utils]))

(defn- gather-params [req]
  (merge
   (:params req)
   (:filter (:params req))))   ;; to accomodate json-api's putting query params into filters.

(defn make-query [subsystem-name query-name request]
  (let [customer-id (auth/request->customer-id request)
        query {:request request
               :query {:subsystem subsystem-name
                       :query-name query-name
                       :params (gather-params request)
                       :customer-id customer-id
                       }
               }
        ]
    (if customer-id
      (assoc query :customer-id customer-id)
      query)))

