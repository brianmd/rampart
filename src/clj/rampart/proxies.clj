(ns rampart.proxies
  (:require
            [rampart.proxies.rosetta :as rosetta]
            ))

(defn process-proxy [query]
  (rosetta/rosetta-proxy query))
