(ns rampart.routes.api
  (:require [rampart.layout :as layout]
            [compojure.core :refer [defroutes GET context]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]

            [cheshire.core :as cheshire]
            [ring.handler.dump :as dump]

            [rampart.process-api :as api]
            [rampart.web-query :as query]

            [rampart.authorization :as auth]

            [summit.utils.core :as utils]))

(defn dump-page [req]
  (dump/handle-dump req)
  )

;; (defn- gather-params [req]
;;   (merge
;;    (:params req)
;;    (:filter req)))
;;    ;; (:filter (:params req))))


(defroutes api-routes-v2
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
      (api/process
       (query/make-query :project
                         :project
                         (assoc req
                                :uri (clojure.string/replace (:uri req) #"api" "api/v2")))))
    (GET "/projects" req
      (api/process
       (query/make-query :project
                         :projects
                         (assoc req
                                :uri "/api/v2/projects"))))


    (context "/:version-num" []
      (GET "/do-auth/:val" req
        (let [v (-> req :params :val)]
          (println "do-auth " @api/do-auth? "<=" v)
          (reset! api/do-auth? (= v "true"))))

      (GET "/accounts/:account-id/projects/:id" req
        (api/process (query/make-query :project :project req)))

      (GET "/accounts/:account-id/projects" req
        (api/process (query/make-query :project :projects req)))

      (GET "/projects/:id" req
        (api/process (query/make-query :project :project req)))

      (GET "/projects" req
        (api/process (query/make-query :project :projects req)))

      (GET "/orders/:id" req
        (api/process (query/make-query :order :order req)))
      )))
