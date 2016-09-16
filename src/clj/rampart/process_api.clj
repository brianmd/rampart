(ns rampart.process-api
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [slingshot.slingshot :refer [throw+]]
            [rampart.config :refer [env]]

            [summit.utils.core :as utils]
            [rampart.authorization :as auth]
            [rampart.proxies :as proxies]
            ))

(defn timenow [] (java.time.LocalDateTime/now))

(defn- prepare-query [query]
  (assoc query
         :query-start-time (timenow)))

(defn- pre-validate [query]
  query)

(defn- pre-authorize [query]
  ;; (let [cust-id 28
  ;;       acct-num 1000736
  ;;       subsystem :project]
  ;;   (if-not (auth/authorized? cust-id acct-num subsystem)
  ;;     (throw+ {:type :not-authorized})))
  query)

(defn- proxy-request [query]
  (proxies/process-proxy query))

(defn- post-validate [query]
  query)

(defn add-body-object [query]
  (if-let [obj (:body-object query)]
    query
    (let [body (-> query :response :body parse-string)]
      (assoc query :body-object body))))

(defn- post-authorize [query]
  (let [cust-id (:customer-id query)
        acct-num (get-in query [:body-object "data" "relationships" "account" "data" "id"])
        acct-num (if acct-num (utils/->int acct-num))
        subsystem :project
        ]
    (println cust-id acct-num subsystem)
    (when-not acct-num
      (utils/ppn (:body-object query))
      (throw+ {:type :not-found :message "no account return"}))
    (when-not (auth/authorized? cust-id acct-num subsystem)
      (throw+ {:type :not-authorized}))
    )
  query)

(defn- finalize-query [query]
  query)

(defn- format-response [query]
  (println "in format-response")
  (let [request (:request query)
        response (:response query)
        full-body (:body response)
        ;; flags (:flags (:params request))
        ;; full-body (parse-string full-body)
        ;; meta-body (if (= "1" (:meta flags)) full-body (dissoc full-body :meta))
        ;; body (if (= "1" (:raw flags)) meta-body (dissoc meta-body :raw))
        body full-body
        body (:body-object query)
        body (assoc body :query (:query query))
        ]
    (println "boo format-response")
    (if response
      {:status (:status response)
       :body body
       ;; :body response
       :headers {"Content-Type" "application/json; charset=utf-8"}
       }
      {:status 400
       :body {:errors ["nil was returned"]}})))



(defn process [query]
  (->>
   query
   ;; utils/ppl
   prepare-query
   (utils/ppbl "after prep")
   pre-validate
   pre-authorize
   (utils/ppbl "after pre-auth")
   proxy-request
   add-body-object
   post-validate
   post-authorize
   (utils/ppbl "after post-auth")
   finalize-query
   format-response))


;; (mount/start)

;; (clojure.pprint/pprint client/default-middleware)
