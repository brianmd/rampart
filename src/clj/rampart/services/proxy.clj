(ns rampart.services.proxy
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
