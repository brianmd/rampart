(ns rampart.process-query
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
            [rampart.services :as services]
            ))

(def do-auth? (atom true))
(defn perform-authorization? []
  @do-auth?)

(defn timenow [] (java.time.LocalDateTime/now))

(defn- prepare-query [query-request]
  (assoc query-request
         :query-start-time (timenow)))

(defn- pre-validate [query-request]
  query-request)

(defn- pre-authorize [query-request]
  (if false  ;;perform-authorization?
    (let [cust-id (:customer-id (:query query-request))
          acct-num (get-in query-request [:result "data" "relationships" "account" "data" "id"])
          acct-num (if acct-num (utils/->int acct-num))
          subsystem (:subsystem query-request)
          ]
      (println "\n\ncust, acct, subsystem:" cust-id acct-num subsystem)
      (when-not acct-num
        (utils/ppn (:result query-request))
        (throw+ {:type :not-found :message "no account return"}))
      (when-not (auth/authorized? cust-id acct-num subsystem)
        (throw+ {:type :not-authorized}))
      ))
  ;; (let [cust-id 28
  ;;       acct-num 1000736
  ;;       subsystem :project]
  ;;   (if-not (auth/authorized? cust-id acct-num subsystem)
  ;;     (throw+ {:type :not-authorized})))
  query-request)

(defn process-query [query-request]
  (services/process-service query-request))

(defn add-result [query-request]
  (if (:result query-request)
    query-request
    (let [body (-> query-request :response :body parse-string)]
      (assoc query-request :result body))))

(defn- post-validate [query-request]
  query-request)

(defn extract-relationships [json-api-map]
  (let [data (or (:data json-api-map) (json-api-map "data"))
        data (if (map? data) [data] data)
        ;; relationships (map #((vals (% "relationships")) "data") data)
        relationships (map #(vals (% "relationships")) data)
        datas (map #(vals %) (flatten relationships))
        ]
    (set (flatten datas))
    ))

(defn extract-account-relationships [json-api-map]
  (->>
   (extract-relationships json-api-map)
   (filter #(= "account" (% "type")))
   (map #(utils/->long (% "id")))
   ))

(defn default-post-authorize-fn [query-request account-nums]
  (let [cust-id (-> query-request :query :customer-id)
        subsystem (-> query-request :query :subsystem)]
    (println "post-auth-fn account-nums:" account-nums ", cust-id: " cust-id)
    (when (empty? account-nums)
      (utils/ppn "no account numbers returned" (:result query-request))
      (throw+ {:type :not-found :message "no account return"}))
    (doseq [acct-num account-nums]
      (println "checking " acct-num " ...")
      (when-not (auth/authorized? cust-id acct-num subsystem)
        ;; (println "    nope :(")
        (throw+ {:type :not-authorized})
        )
      )))

(defn- post-authorize [query-request]
  (println "keys" (keys query-request))
  (println "keys2" (keys (:result query-request)))
  (let [authorize (:post-authorize? (:query-def query-request))
        authorize-fn (or (:post-authorize-fn (:query-def query-request)) default-post-authorize-fn)]
    (if true ;(and (perform-authorization?) authorize-fn)
      (let [q            (:query query-request)
            cust-id      (:customer-id q)
            subsystem    (:subsystem q)
            body         (:result query-request)
            account-nums (extract-account-relationships body)
            ]
        (println "\n\ncust, accts, subsystem:" cust-id account-nums subsystem (keys query-request) q)
        (println "query keys:" (keys query-request))
        (println "request keys:" (keys (:request query-request)))
        (println "\nheader keys:" (keys (:headers (:request query-request))))
        (println "authorization:" (get-in query-request [:request :headers "authorization"]))
        (println "\n")
        (authorize-fn query-request account-nums)
        )))
  query-request)

(defn- finalize-query [query-request]
  (assoc query-request
         :query-stop-time (timenow)))


(defn- run [query-request fn-name args]
  (prn fn-name)
  query-request)

(defn- wrap [query-request fn-var & args]
  (println fn-var)
  (let [fn-name-str (str (:name (meta fn-var)))]
    (println "\n\n\n-------------------" fn-name-str "\n\n")
    (->
     query-request
     (run (keyword (str "pre-" fn-name-str)) args)
     fn-var
     (run (keyword (str "post-" fn-name-str)) args)
     )))

(defn process [query-request]
  (->
   query-request
   ;; utils/ppl
   prepare-query
   ;; (utils/ppbl "after prep")
   pre-validate
   pre-authorize
   ;; (utils/ppbl "after pre-auth")
   (wrap #'process-query)
   add-result
   post-validate
   post-authorize
   ;; (utils/ppbl "after post-auth")
   finalize-query
   ))


;; (mount/start)

;; (clojure.pprint/pprint client/default-middleware)
