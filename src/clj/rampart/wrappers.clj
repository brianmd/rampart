(ns rampart.wrappers
  (:require [slingshot.slingshot :refer [try+ throw+]]
            [clojure.tools.logging :as log]
            ))

(defn wrap-logger [handler]
  (fn [request]
    (println "\n\n---------\nrequested url:" (:uri request)
             " with params " (:params request))
    (let [response (handler request)]
      (println "wrapper response status :" (:status response))
      response)
    ))

(defn- error-response [err err-num err-msg]
  (println "caught error during http processing: " err-msg "(" err-num ")")
  (log/error (:throwable err) "caught error during http processing: " err-msg)
  {:status err-num :body {:errors [err-msg]}}
  )

(defn wrap-error [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:type :not-authorized] {}
       (error-response &throw-context 403 "Not authorized"))

     (catch [:type :not-authorized] {}
       (error-response &throw-context 403 "Forbidden"))

     (catch [:type :not-found] {}
       (error-response &throw-context 404 "Not found"))

     (catch [:type :unavailable] {}
       (error-response &throw-context 503 "Service unavailable"))

     (catch [:type :server-unavailable] {}
       (error-response &throw-context 504 "Gateway timeout"))

     (catch Object _
       (error-response &throw-context 500 "Unknown error"))
     )))

