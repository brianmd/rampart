(ns rampart.wrappers
  (:require [slingshot.slingshot :refer [try+ throw+]]
            [clojure.tools.logging :as log]
            ))

(defonce wrap-errors? (atom true))
(defn set-wrap-errors [bool]
  (reset! wrap-errors? bool))

(defn- format-response [query-request]
  (println "in format-response")
  (println "query-request keys" (keys query-request))
  (if (:response query-request)
    (let [request (:request query-request)
          response (:response query-request)
          full-body (:body response)
          ;; flags (:flags (:params request))
          ;; full-body (parse-string full-body)
          ;; meta-body (if (= "1" (:meta flags)) full-body (dissoc full-body :meta))
          ;; body (if (= "1" (:raw flags)) meta-body (dissoc meta-body :raw))
          body full-body
          body (:result query-request)
          ;; body (assoc body :query (:query query-request))
          ]
      ;; (println "format-response query:" (:query query-request))
      ;; (println "body" (:body response))
      ;; (println "body type" (type (:body response)))
      (if response
        {:status (:status response)
         :body body
         ;; :body response
         :headers {"Content-Type" "application/json; charset=utf-8"}
         }
        ;; should probably throw an error so all errors are handled in the same place
        {:status 404
         :body {:errors ["nil was returned"]}}))
    query-request   ;; not a query-request -- return whatever was provided
    ))

(defn wrap-formatter [handler]
  (fn [request]
    (format-response (handler request))))

(defn wrap-logger [handler]
  (fn [request]
    (println "\n\n---------\nrequested url:" (:uri request)
             " with params " (:params request))
    (let [response (handler request)]
      (println "wrapper response status :" (:status response))
      response)))

(defn- error-response [err err-num err-msg]
  (println "\n\n.....................\n------------ caught error during http processing: " err-msg "(" err-num ")\n..................\n\n")
  (log/error (:throwable err) "caught error during http processing: " err-msg)
  {:status err-num :body {:errors [err-msg]}}
  )

(defn wrap-error [handler]
  (fn [request]
    (if @wrap-errors?
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
       )
      (handler request))))

;; (set-wrap-errors false)
