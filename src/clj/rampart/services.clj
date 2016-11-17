(ns rampart.services
  (:require
            [rampart.services.rosetta :as rosetta]
            ))

;; currently dead simple as there is only one backend service
(defn process-service [query-request]
  (println "in process-service (before rosetta/services)")
  (rosetta/http-request query-request))
