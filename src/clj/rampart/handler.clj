(ns rampart.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [rampart.layout :refer [error-page]]
            [rampart.routes.home :refer [home-routes]]
            [compojure.route :as route]
            [rampart.env :refer [defaults]]
            [mount.core :as mount]
            [rampart.middleware :as middleware]

            [rampart.routes.api :refer [api-routes-v2]]
            [rampart.wrappers :as ramwrap]
            ))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (->
        ;; #'home-routes
        #'api-routes-v2
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats)
        (wrap-routes ramwrap/wrap-error)
        (wrap-routes ramwrap/wrap-logger)
        (wrap-routes ramwrap/wrap-formatter)
        )
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
