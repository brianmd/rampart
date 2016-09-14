(ns rampart.process-api
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [rampart.config :refer [env]]
            ))

(def debug-options
  {:debug true
   :debug-body true})

(def ^:private rosetta-conn-pool
  (conn-mgr/make-reusable-conn-manager {:timeout 360 :threads 10}))

(defn reconstitute-uri [base-url request]
  (let [query-str (if-let [q (:query-str request)]
                    (str "&" q)
                    "")
        ]
    (str base-url (:uri request) query-str)
    ))





(defn- rosetta-proxy [query]
  (let [request (:request query)
        method (:request-method request)
        ;; base-url (:rosetta-url (cprop.source/from-env))
        base-url (-> env :rosetta-url)
        url (reconstitute-uri base-url request)
        response (client/request
                  (merge
                   {:method method
                    :url url
                    :connection-manager rosetta-conn-pool
                    ;; :as :json
                    ;; :accept :json
                    :x-forwarded-for (-> request :headers :x-forwarded-for)
                    :query-params (:query-params request)
                    :form-params (:form-params request)
                    }
                   debug-options
                   ))
        ]
    (assoc query :response response)
    ))


;; (defn rosetta-proxy [query]
;;   (assoc query :response
;;          {:status 200
;;           :body {:a :ok}}))





(defn- authorize-request [query]
  query)

(defn- parse-query [query]
  query)

(defn- authorize-response [query]
  query)

(defn- proxy-request [query]
  (rosetta-proxy (:request query)))

(defn- validate-response [query]
  query)

(defn- authorize-response [query]
  query)

(defn- format-response [query]
  (:response query))

(defn process [request]
  (->
   {:request request}  ;; the query
   authorize-request
   parse-query
   authorize-request
   proxy-request
   validate-response
   authorize-response
   format-response))


;; (mount/start)

;; (clojure.pprint/pprint client/default-middleware)
