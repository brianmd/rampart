(ns rampart.routes.api
  (:require [rampart.layout :as layout]
            [compojure.core :refer [defroutes GET context]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]

            [cheshire.core :as cheshire]
            [ring.handler.dump :as dump]

            [rampart.process-query :refer [process do-auth?]]
            [rampart.web-query :refer [make-query-request]]

            [rampart.authorization :as auth]
            [rampart.wrappers :as wrappers]
            [rampart.services.rosetta-helpers :as helpers]

            [summit.utils.core :as utils]
            [rampart.services.rosetta :refer [http-request]]
            [rampart.services.proxy :refer [proxy]]
            [rampart.spreadsheet :as spreadsheet]

            [clojure.string :as str]))

;; :extract-account-nums (fn [m] (println "in extract") (pr (keys m)) (vector (:account-num m)))

;; (process
;;  (make-query-request :project
;;                      :project
;;                      {:uri "/api/v2/project/3?filter[account]=1037657&env[server]=prd&env[pw]=abcd&env[customer-id]=28"
;;                       :request-method :get
;;                       :params {:id 3 :account "1037657" :env {:server "prd" :pw "abcd" :customer-id "28"}}
;;                       }))

;; (process
;;  (make-query-request :project
;;                      :project-spreadsheet-data
;;                      {:uri "/api/v2/project-spreadsheet-data/3?filter[account]=1037657&env[server]=prd&env[pw]=abcd&env[customer-id]=28"
;;                       :request-method :get
;;                       :params {:id 3 :account "1037657" :env {:server "prd" :pw "abcd" :customer-id "28"}}
;;                       }))

;; (make-query-request :project
;;                     :project
;;                      {:uri "/api/v2/projects/3?filter[account]=1037657&env[server]=prd&env[pw]=abcd&env[customer-id]=28"
;;                       :request-method :get
;;                       :params {:id 3 :account "1037657" :env {:server "prd" :pw "abcd" :customer-id "28"}}
;;                       })

;; (do
;;   (rampart.process-query/process
;;    {
;;     ;; :request {:uri "/api/v2/projects/3" :query-string "account=1037657"}
;;     :query {:subsystem :project
;;             :query-name :project
;;             :params {:id 3 :account-num "1037657"}
;;             :server "prd"
;;             :customer-id "28"
;;             }})
;;   nil)

;; (rampart.process-query/process
;;  {:request {:uri "/api/v2/projects/3" :query-string "account=1037657"}
;;   :query {:subsystem :project
;;           :query-name :projects
;;           :params {:account-num 1037657}
;;           :server "prd"
;;           :customer-id 28
;;           }})

(defn ok-json [m]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body m})

(defn dump-page [req]
  (dump/handle-dump req)
  )

;; (defn- gather-params [req]
;;   (merge
;;    (:params req)
;;    (:filter req)))
;;    ;; (:filter (:params req))))


(defroutes api-routes-v2
  (GET "/error/:bool" req
    (let [bool (= "true" (-> req :params :bool))]
      (wrappers/set-wrap-errors bool)
      {:body {:set-wrap-errors bool}}))
  (GET "/error*" req (/ 1 0)) ;; force error to show debugger in browser
  (GET "/dump*" req (println "\n\n\n-------------\n\n\n") (dump-page req))
  (GET "/customers/:cust-id/subsystems" req
    (let [params (:params req)
          cust-id (utils/->long (:cust-id params))
          global-subsystems (auth/customer-subsystems cust-id)
          subsystems (auth/all-customer-account-subsystems cust-id)
          ]
      {:status 200
       :accept :json
       :body (merge {:global global-subsystems} subsystems)
       }))
  (GET "/customers/:cust-id/accounts/:acct-id/subsystems" req
    (let [params (:params req)
          subsystems
          (auth/customer-account-subsystems (utils/->long (:cust-id params)) (utils/->long (:acct-id params)))
          ]
      {:status 200
       :accept :json
       ;; :body [(:cust-id params) :acct-id params] ;subsystems
       ;; :body (vector (utils/->long (:cust-id params)) (utils/->long (:acct-id params)))
       :body subsystems
       }))

  (context "/api" []
    (GET "/projects/:id" req
      (process
       (make-query-request :project
                         :project
                         (assoc req
                                :uri (clojure.string/replace (:uri req) #"api" "api/v2")))))
    (GET "/projects" req
      (process
       (make-query-request :project
                         :projects
                         (assoc req
                                :uri "/api/v2/projects"))))


    (context "/:version-num" []
      (GET "/default-server" req
           (let [query-request (process
                                (make-query-request :status
                                                    :default-server
                                                    (assoc req
                                                           :uri "/api/v2/default-server")))]
             (ok-json {:result (:result query-request)})
             ))
      (GET "/do-auth/:val" req
        (let [v (-> req :params :val)]
          (println "do-auth " @do-auth? "<=" v)
          (reset! do-auth? (= v "true"))))

      (GET "/accounts/:account/projects/:id" req
        (process (make-query-request :project :project req)))

      (GET "/accounts/:account/projects" req
        (process (make-query-request :project :projects req)))

      (GET "/project-spreadsheet/:id" req
           (let [data (process (make-query-request :project :project-spreadsheet-data req))
                 result (:result data)
                 filepath (spreadsheet/create-temp-spreadsheet "amps" "xlsx" (result "headers") (result "data"))
                 filename (last (str/split filepath #"/"))]
             (println "filename::" filename)
             {:status 200
              :headers {"Content-Type" spreadsheet/mime-spreadsheet
                        "Content-Disposition" (str "inline; filename\"" filename "\"")}
              :body (io/file filepath)
              }))

      (GET "/project-spreadsheet-data/:id" req
        (process (make-query-request :project :project-spreadsheet-data req)))

      (GET "/projects/:id" req
        (process (make-query-request :project :project req)))

      (GET "/projects" req
        (process (make-query-request :project :projects req)))

      (GET "/orders/:id" req
        (process (make-query-request :order :order req)))

      (GET "/*" req
           (ok-json
            (utils/req-sans-unprintable req)
            ;; {:at-root true}
            ))
      ))
  (GET "/*" req
       (proxy req)
       ;; (ok-json
       ;;  (utils/req-sans-unprintable req))
       )
  )
