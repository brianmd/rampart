(ns rampart.routes.api
  (:require [rampart.layout :as layout]
            [compojure.core :refer [defroutes GET context]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]

            [cheshire.core :as cheshire]
            [ring.handler.dump :as dump]

            [rampart.process-api :as api]
            [rampart.web-query :as query]
            ))

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
