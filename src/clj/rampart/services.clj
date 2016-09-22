(ns rampart.services
  (:require
            [rampart.services.rosetta :as rosetta]
            ))

(defn process-service [query]
  (rosetta/services query))
