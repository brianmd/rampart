(ns rampart.services.rosetta
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [rampart.config :refer [env]]

            [rampart.services.core :as core]

            [summit.utils.core :as utils]))

;; (def ^:private rosetta-conn-pool
;;   (conn-mgr/make-reusable-conn-manager {:timeout 360 :threads 10}))

(defn services [query]
  (let [request (:request query)
        method (:request-method request)
        ;; base-url (:rosetta-url (cprop.source/from-env))
        base-url (-> env :rosetta-url)
        url (core/reconstitute-uri base-url request)
        _ (utils/ppn "url:" url method core/debug-options)
        response (client/request
                  (merge
                   {:method method
                    :url url
                    ;; :connection-manager rosetta-conn-pool
                    ;; :as :json
                    ;; :accept :json
                    ;; :x-forwarded-for (-> request :headers :x-forwarded-for)
                    ;; :query-params (:query-params request)
                    ;; :form-params (:form-params request)
                    }
                   core/debug-options
                   ))
        _ (utils/ppn "response:" )
        ]
    (assoc query :response response)
    ))


;; (defn rosetta-service [query]
;;   (assoc query :response
;;          {:status 200
;;           :body {:a :ok}}))



(def ^:private service-query-defs
  [
   {:name :project
    :post-authorize? true
    }
   {:name :projects
    :pre-authorize? true
    :post-authorize? true
    }
   {:name :order
    :post-authorize? true
    }
   ])

(def ^:private query-def-defaults
  {:pre-authorize? nil
   :post-authorize? nil
   :service #'services})

(defn- make-query-def [query]
  [(:name query) (merge query-def-defaults query)])

(defn- make-query-defs [queries]
  (into {} (map #(make-query-def %) queries)))

(def query-definitions (make-query-defs service-query-defs))

