(ns rampart.web-query
  (:require [clojure.string :as str]

            ;; [mount.core :as mount]
            ;; [rampart.db.core :as db]

            ;; [buddy.sign.jwt :as jwt]
            ;; [cheshire.core :refer [generate-string parse-string]]
            ;; [clj-http.client :as client]
            ;; [clj-http.conn-mgr :as conn-mgr]
            ;; [rampart.config :refer [env]]

            [rampart.authorization :as auth]

            [summit.utils.core :as utils]))

(defn- gather-params [req]
  (merge
   (:params req)
   (:filter (:params req))))   ;; to accomodate json-api's putting query params into filters.
   ;; (:filter req)))   ;; to accomodate json-api's putting query params into filters.

(defn make-query [subsystem query-name request]
  (let [customer-id (auth/request->customer-id request)
        query {:request request
               :query {:subsystem subsystem
                       :query-name query-name
                       :params (gather-params request)
                       }
               }
        ]
    (if customer-id
      (assoc query :customer-id customer-id)
      query)))

