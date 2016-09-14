(ns rampart.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[rampart started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[rampart has shut down successfully]=-"))
   :middleware identity})
