(ns rampart.proxies.rosetta
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [rampart.config :refer [env]]

            [rampart.proxies.core :as proxy]

            [summit.utils.core :as utils]))

(def ^:private rosetta-conn-pool
  (conn-mgr/make-reusable-conn-manager {:timeout 360 :threads 10}))

(defn rosetta-proxy [query]
  (let [request (:request query)
        method (:request-method request)
        ;; base-url (:rosetta-url (cprop.source/from-env))
        base-url (-> env :rosetta-url)
        url (proxy/reconstitute-uri base-url request)
        _ (utils/ppn "url:" url)
        response (client/request
                  (merge
                   {:method method
                    :url url
                    :connection-manager rosetta-conn-pool
                    ;; :as :json
                    ;; :accept :json
                    ;; :x-forwarded-for (-> request :headers :x-forwarded-for)
                    ;; :query-params (:query-params request)
                    ;; :form-params (:form-params request)
                    }
                   proxy/debug-options
                   ))
        _ (utils/ppn "response:" )
        ]
    (assoc query :response response)
    ))


;; (defn rosetta-proxy [query]
;;   (assoc query :response
;;          {:status 200
;;           :body {:a :ok}}))

(def project-fn
  {:name :project
   :pre-validate (fn [query])
   ;; :pre-authorize (fn [customer query] (authorize customer-id (:account-num query) :project))
   :service rosetta-proxy
   })

;; (def proxy
;;   {:name :rosetta
;;    :proxy rosetta-proxy
;;    ;; :pre-validate 
;;    })

