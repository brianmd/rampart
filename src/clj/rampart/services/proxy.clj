(ns rampart.services.proxy
  (:require [clojure.string :as str]

            [mount.core :as mount]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :as json :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.string :as string]
            [datascript.core :as d]

            [rampart.db.core :as db]
            [rampart.config :refer [env]]

            [rampart.services.core :as core]

            [summit.utils.core :as utils]
            ))

(def schema
  {:proxy/service-instance {:db/valueType :db.type/ref}
   :proxy/instance-name {:db/unique :db.unique/identity}
   :proxy/from-uri {:db/cardinality :db.cardinality/many}})

(def db (d/create-conn schema))

(def default-services
  [
   {:db/id -1
    :service/instance-name :blue-harvest.dev
    :service/service :blue-harvest
    :service/env :prod
    :service/uri "http://marketing-02.insummit.com:7442"}
   {:db/id -2
    :service/instance-name :blue-harvest.prod
    :service/service :blue-harvest
    :service/env :dev
    :service/uri "http://marketing-22.insummit.com:7442"}
   {:db/id -3
    :service/instance-name :project.brian
    :service/service :projects.10.9.0.124
    :service/env :brian
    :service/uri-prefix "http://10.9.0.124:3000"}
   ])

(d/transact! db default-services)

(d/q '[:find ?e ?name
       :where [?e :service/instance-name ?name]]
     @db)

(def default-redirects
  {:ipaddr :default
   :proxy/routes
   [{:proxy/from-uri ["/api/v2/releases" "/api/v2/release-line-items"]
     :service/instance-name :project.brian
     :proxy/to-uri "http://10.9.0.124:3000/api/v2/release"
     }
    {:proxy/from-uri ["/api/v2/projects" "/api/v2/project-line-items"]
     :proxy/to-uri "http://mark-docker01.insummit.com:3000/api/v2/release"}
    {:proxy/from-uri ["/api/v2"]
     }
    ]})

(defn find-redirect
  [uri ipaddr]
  (let [tokens (string/split uri #"\/")]
    tokens)
  )
(find-redirect "/api/v2/release" "10.9.0.124")
(find-redirect "/api/v2/release" :default)
(string/join "/" (find-redirect "/api/v2/release" "10.9.0.124"))

(defn proxy
  [req]
  (let [
        ;; use remote-addr to override where to send requests.
        ;; for example, while developing, steve may want uri's
        ;; starting with /api/v2/rails to route to port 3000 on
        ;; his laptop
        remote-addr (:remote-addr req)

        method (:request-method req)
        ;; scheme "http"
        ;; host "finch.insummit.com:3003"
        scheme "https"
        host "www.google.com"
        url (cond->
                (str scheme "://" host)
              (:uri req) (str (:uri req))
              (:query-string req) (str "?" (:query-string req))
              )
        ;; headers (assoc (:headers req) :host host)
        headers (dissoc (:headers req) "host" "cookie")
        _ (prn headers)
        form-params (:form-params req)
        http-params (cond->
                        {:method method
                         :url url
                         }
                      ;; headers (assoc :headers headers)
                      ;; form-params (assoc :form-params form-params)
                      )
        ]
    (prn http-params)
    (let [response (client/request http-params)]
      (prn response)
      (println "\n\n\n ------------  response keys")
      (prn (-> response keys))
      (dissoc response :headers)
      {:status 200
       :headers (:headers response)
       :body (:body response)
       }
      (select-keys response [:status :headers :body])
      )))

;; (client/request {:method :get :url "https://www.google.com/"})


;; (proxy
;;     {:uri "/api/v2/projects/3"
;;      :query-string "filter[account]=1037657&env[server]=prd&env[pw]=abcd&env[customer-id]=28"
;;      :request-method :get
;;      })
