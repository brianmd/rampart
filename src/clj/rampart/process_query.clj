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

            [rampart.services.rosetta :as rosetta]))

(def do-auth? (atom true))
(defn perform-authorization? []
  @do-auth?)

(defn- dlog [& args] (apply println args))

(defn timenow [] (java.time.LocalDateTime/now))

(defn- prepare-query [{:keys [query]
                       :as query-request}]
  (dlog "in prepare-query")
  (let [qname (:query-name query)
        qdef  (qname rosetta/query-definitions)]
    (assoc query-request
           :query-start-time (timenow)
           :query (merge qdef query)
           )))
;; (:projects rosetta/query-definitions)

(defn- pre-validate [query-request]
  (dlog "in pre-validate")
  query-request)

(defn- pre-authorize [query-request]
  (dlog "in pre-authorize")
  (when (-> query-request :query :pre-authorize?)
    (let [query (:query query-request)
          cust-id (:customer-id query)
          acct-num-str (-> query :params :account)
          acct-num (if acct-num-str (utils/->int acct-num-str))
          subsystem (query :subsystem)
          ]
      (println "\n\ncust, acct, subsystem:" cust-id acct-num subsystem)
      (when-not acct-num
        (println "no account return (pre-auth)")
        (throw+ {:type :not-found :message "no account return (pre-auth)"}))
      (when-not (auth/authorized? cust-id acct-num subsystem)
        (println "not authorized (pre-auth)")
        (throw+ {:type :not-authorized}))
      ))
  query-request)

(defn process-query [query-request]
  (dlog "in process-query")
  (services/process-service query-request))

(defn add-result [query-request]
  (if (:result query-request)
    query-request
    (let [body (-> query-request :response :body parse-string)]
      (assoc query-request :result body))))

(defn- post-validate [query-request]
  query-request)

(defn extract-relationships [json-api-map]
  (dlog "in extract-relationships")
  (let [data (or (:data json-api-map) (json-api-map "data"))
        data (if (map? data) [data] data)
        ;; relationships (map #((vals (% "relationships")) "data") data)
        relationships (map #(vals (% "relationships")) data)
        datas (map #(vals %) (flatten relationships))
        set-data (-> datas flatten set)
        ]
    set-data
    ))

(defn extract-account-relationships [json-api-map]
  (->>
   (extract-relationships json-api-map)
   (filter #(= "account" (% "type")))
   (map #(utils/->long (% "id")))
   (filter (complement nil?))
   ))

(defn default-post-authorize-fn [query-request account-nums]
  (when (-> query-request :query :post-authorize?)
    (let [cust-id (-> query-request :query :customer-id)
          subsystem (-> query-request :query :subsystem)]
      (println "post-auth-fn account-nums:" account-nums ", cust-id: " cust-id)
      (prn [account-nums cust-id])
      (when (empty? account-nums)
        (println (str "no account numbers returned. result keys: " (-> query-request :result keys)))
        (throw+ {:type :not-found :message "no account return (post-auth)"}))
      (doseq [acct-num account-nums]
        (println "checking " acct-num " ...")
        (when-not (auth/authorized? cust-id acct-num subsystem)
          (throw+ {:type :not-authorized})
          )
        ))))

(defn- post-authorize [query-request]
  (dlog "in post-authorize")
  (let [q (:query query-request)
        authorize (:post-authorize? q)
        authorize-fn (or (:post-authorize-fn q) default-post-authorize-fn)
        extract-account-nums (or (:extract-account-nums q) extract-account-relationships)]
    (when (and (-> query-request :query :post-authorize?) authorize-fn)
      (let [
            cust-id      (:customer-id q)
            subsystem    (:subsystem q)
            body         (:result query-request)
            account-nums (extract-account-nums body)
            ]
        (println "\n\ncust, accts, subsystem:" cust-id account-nums subsystem (keys query-request) q)
        (authorize-fn query-request account-nums)
        )))
  query-request)

(defn- finalize-query [query-request]
  (dlog "in finalize-query")
  (assoc query-request
         :query-stop-time (timenow)))


(defn- run [query-request fn-name args]
  query-request)

(defn- wrap-pre-post [query-request fn-var & args]
  (println "fn-var" (meta fn-var))
  (let [fn-name-str (str (:name (meta fn-var)))]
    (let [result
          (->
           query-request
           (run (keyword (str "pre-" fn-name-str)) args)
           fn-var
           (run (keyword (str "post-" fn-name-str)) args)
           )]
      result)))

(defn process [query-request]
  (->
   query-request
   ;; utils/ppl
   prepare-query
   ;; (utils/ppbl "after prep")
   pre-validate
   pre-authorize
   ;; (utils/ppbl "after pre-auth")
   (wrap-pre-post #'process-query)
   add-result
   post-validate
   post-authorize
   ;; (utils/ppbl "after post-auth")
   finalize-query
   ))




;; (mount/start)

;; (clojure.pprint/pprint client/default-middleware)
