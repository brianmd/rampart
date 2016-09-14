(ns rampart.routes.api
  (:require [rampart.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]

            [cheshire.core :as cheshire]
            [ring.handler.dump :as dump]

            [rampart.process-api :as api]
            ))

(defn dump-page [req]
  ;; (println req)
  (dump/handle-dump req)
  ;; {:status 200
  ;;  ;; :body {:a 3}}
  ;;  :body {:request req}}
  )

(defroutes api-routes
  (GET "/dump*" req (println "\n\n\n-------------\n\n\n") (dump-page req))
  ;; (GET "/" [req] (println "dump request") (dump-page req))
  (GET "/" req (println "dump request")
       (api/process req)
       ;; {:status 200
       ;;  :body {:not :okay}
       ;;  ;; :body (cheshire/generate-string req)
       ;;  ;; :body (compojure.response/render req)
       ;;  }
       )
  ;; (GET "/" [] (-> (response/ok (-> "docs/docs.md" io/resource slurp))
  ;;                 (response/header "Content-Type" "text/plain; charset=utf-8")))
  )
