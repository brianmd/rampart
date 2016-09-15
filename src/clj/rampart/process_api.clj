(ns rampart.process-api
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [rampart.config :refer [env]]

            [summit.utils.core :as utils]
            [rampart.authorization :as auth]
            [rampart.proxies :as proxies]
            ))

(defn timenow [] (java.time.LocalDateTime/now))

(defn- prepare-query [query]
  (assoc query
         :query-start-time (timenow)))

(defn- pre-validate [query]
  query)

(defn- pre-authorize [query]
  (println "\n\n\n\n @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ \n\n\n")
  (println (auth/customer-subsystems 28))
  query)

(defn- authorize-response [query]
  query)

(defn- proxy-request [query]
  (proxies/process-proxy query))

(defn- post-validate [query]
  query)

(defn- post-authorize [query]
  query)

(defn- finalize-query [query]
  query)

(defn- format-response [query]
  (let [response (:response query)]
    (if response
      {:status (:status response)
       :body (:body response)
       :headers {"Content-Type" "application/json"}
       }
      {:status 400
       :body {:errors ["nil was returned"]}})))



(defn process [query]
  (->
   query
   utils/ppl
   prepare-query
   pre-validate
   pre-authorize
   proxy-request
   post-validate
   post-authorize
   finalize-query
   format-response))


;; (mount/start)

;; (clojure.pprint/pprint client/default-middleware)
