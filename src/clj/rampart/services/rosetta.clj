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

            [summit.utils.core :as utils]))

;; (def ^:private rosetta-conn-pool
;;   (conn-mgr/make-reusable-conn-manager {:timeout 360 :threads 10}))

;; (rampart.process-query/process {:request {:request-method :get :uri "/api/v2/projects/3"}
;;                                 :query {:subsystem :project
;;                                         :query-name :projects
;;                                         :params {:account-num 1037657}
;;                                         :server "prd"
;;                                         :customer-id 28
;;                                         }})

;; clj-http.client/default-middleware
;; clj-http.client/wrap-form-params

(defn http-request [{:keys [query]
                     :as query-request}]
  ;; (println "uri-fn" (:uri-fn query-def) (keys query-request) (keys (:query-def query-request)) (:query-name (:query query-request)))
  ;; #break (keys query-request)
  (let [
        base-url (-> env :rosetta-url)
        uri-fn (:uri-fn query)
        url (str base-url (uri-fn query-request))
        _ (println "url:" url)
        method (:method query)
        params (:params query)
        ;; request (:request query-request)      ;; TODO: should not use the incoming http request in the query engine!!!
        ;; method (or (:request-method request) :get)
        ;; _ (println "url" base-url request)
        ;; url (core/reconstitute-uri base-url request)

        ;; method (or (-> query-request :query-def :request-method) :get)
        ;; base-url (:rosetta-url (cprop.source/from-env))
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
        _ (println "http-request http-params:" http-params)
        http-params (merge http-params @core/debug-options)
        ;; _ (utils/ppn "http-params (method ^ url)" http-params)
        ]
    ;; #break http-params
    (let [
        response (client/request http-params)
        ;; _ (utils/ppn "response:" (keys response))
        result (-> response :body json/parse-string)
        ]
      (assoc query-request
             :response response
             :result result))
    ))
;; (def hreq (http-request {:request {:request-method :get :uri "/api/v2/default-server"}}))
;; (keys hreq)
;; (:response hreq)
;; (-> hreq :response :body)
;; (:result hreq)


;; (defn rosetta-service [query]
;;   (assoc query :response
;;          {:status 200
;;           :body {:a :ok}}))



(def ^:private query-def-defaults
  {:pre-authorize? nil
   :post-authorize? nil
   :method :get
   :uri-fn (fn [query] (:uri query))
   ;; :params {}
   :service #'http-request})

(def ^:private service-query-defs
  [
   {:name :default-server
    :format :json
    ;; :params {}
    :uri-fn (fn [_] (str "/api/v2/default-server"))
    }
   {:name :project
    :format :json-api
    :post-authorize? true
    :uri-fn (fn [query-request]
              (println "query-params::::" (:query query-request))
              ;; (let [account-num "1037657"
              ;;       server :prd]
                ;; (str "/api/v2/projects?filter[account]=" account-num "&env[server]=" (utils/->str server))))
                (str "/api/v2/projects/" (-> query-request :query :params :id))
                ;; )
              )
    ;; {:uri (str "/api/v2/projects?filter[account]=" account-num "&env[server]=" (utils/->str server))
    }
   {:name :projects
    :format :json-api
    ;; :pre-authorize? true
    :post-authorize? true
    :uri-fn (fn [query-request]
              ;; (println "query-params::::" (:query query-request))
              ;; (let [account-num "1037657"
              ;;       server :prd]
                ;; (str "/api/v2/projects?filter[account]=" account-num "&env[server]=" (utils/->str server))))
                (str "/api/v2/projects")
                ;; )
              )
    }

   {:name :project-spreadsheet-data
    :format :schema
    :post-authorize? true
    :extract-account-nums (fn [m] (println "in extract") (pr m) (pr (keys m)) (vector (m "account-num")))
    ;; :authorize-fn #(fn [& args]
    ;;                       (println "in post-authorize" args)
    ;;                  true)
    }
   {:name :order
    :format :json-api
    :post-authorize? true
    }
   ])

(defn- make-query-def [query]
  [(:name query) (merge query-def-defaults query)])

(defn- make-query-defs [queries]
  (into {} (map #(make-query-def %) queries)))

(def query-definitions (make-query-defs service-query-defs))

