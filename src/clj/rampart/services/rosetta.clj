;; (System/getenv "abc")
;; (defn lookup- [])

;; (utils/->str :abc)


(ns rampart.services.rosetta
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :as json :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [rampart.config :refer [env]]

            [rampart.services.core :as core]

            [summit.utils.core :as utils]
            ))

;; (def ^:private rosetta-conn-pool
;;   (conn-mgr/make-reusable-conn-manager {:timeout 360 :threads 10}))

;; (rampart.process-query/process {:request {:request-method :get :uri "/api/v2/projects/3"}
;;                                 :query {:subsystem :project
;;                                         :query-name :projects
;;                                         :params {:account-num 1037657}
;;                                         :server "prd"
;;                                         :customer-id 28
;;                                         }})

(defn http-request [{:keys [query]
                     :as query-request}]
  (let [
        base-url (-> env :rosetta-url)
        uri-fn (:uri-fn query)
        url (str base-url (uri-fn query-request))
        method (:method query)
        params (:params query)
        http-params {:method method
                     :url url
                     :query-params params
                     ;; :connection-manager rosetta-conn-pool
                     ;; :as :json
                     ;; :accept :json
                     ;; :x-forwarded-for (-> request :headers :x-forwarded-for)
                     ;; :query-params (:query-params request)
                     ;; :form-params (:form-params request)
                     }
        http-params (merge http-params @core/debug-options)
        ]
    (try
      (let [
            response (client/request http-params)
            result (-> response :body json/parse-string)
            ]
        (assoc query-request
               :response response
               :result result))
      (catch Exception e
        (println "error in http-request, url: " url ", http-params:")
        (prn http-params)
        ;; (println "\n    query-request:")
        ;; (prn query-request)
        (throw e)))
    ))
;; (def hreq (http-request {:request {:request-method :get :uri "/api/v2/default-server"}}))
;; (keys hreq)
;; (:response hreq)
;; (-> hreq :response :body)
;; (:result hreq)


(def ^:private query-def-defaults
  {:pre-authorize? nil
   :post-authorize? nil
   :method :get
   :uri-fn (fn default-uri-fn [query-request] (-> query-request :query :uri))
   :service #'http-request})

(def ^:private service-query-defs
  [
   {:name :default-server
    :format :json
    :uri "/api/v2/default-server"
    }
   {:name :project
    :format :json-api
    :post-authorize? true
    :uri-fn (fn [query-request]
              (str "/api/v2/projects/" (-> query-request :query :params :id)))
    }
   {:name :projects
    :format :json-api
    :pre-authorize? true
    :post-authorize? true
    :uri "/api/v2/projects"
    }

   {:name :project-spreadsheet-data
    :format :schema
    :post-authorize? true
    :extract-account-nums (fn [m]
                            (println "in extract")
                            ;; (pr (keys m))
                            (if-let [acct-num (m "account-num")]
                              (vector acct-num)))
    :uri-fn (fn [query-request]
              (str "/api/v2/project-spreadsheet-data/" (-> query-request :query :params :id)))
    }

   {:name :order
    :format :json-api
    :post-authorize? true
    :uri-fn (fn [query-request]
              (str "/api/v2/orders/" (-> query-request :query :params :id)))
    }
   ])

(defn- make-query-def [query]
  [(:name query) (merge query-def-defaults query)])

(defn- make-query-defs [queries]
  (into {} (map #(make-query-def %) queries)))

(def query-definitions (make-query-defs service-query-defs))

