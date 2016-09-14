(ns user
  (:require [mount.core :as mount]
            [rampart.figwheel :refer [start-fw stop-fw cljs]]
            rampart.core))

(defn start []
  (mount/start-without #'rampart.core/repl-server))

(defn stop []
  (mount/stop-except #'rampart.core/repl-server))

(defn restart []
  (stop)
  (start))


