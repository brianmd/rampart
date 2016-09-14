(ns rampart.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [rampart.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[rampart started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[rampart has shut down successfully]=-"))
   :middleware wrap-dev})
