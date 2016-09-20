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

(def do-auth? (atom true))
(defn perform-authorization? []
  @do-auth?)

(defn timenow [] (java.time.LocalDateTime/now))

(defn- prepare-query [query]
  (assoc query
         :query-start-time (timenow)))

(defn- pre-validate [query]
  query)

(defn- pre-authorize [query]
  (if false  ;;perform-authorization?
    (let [cust-id (:customer-id (:query query))
          acct-num (get-in query [:body-object "data" "relationships" "account" "data" "id"])
          acct-num (if acct-num (utils/->int acct-num))
          subsystem (:subsystem query)
          ]
      (println "\n\ncust, acct, subsystem:" cust-id acct-num subsystem)
      (when-not acct-num
        (utils/ppn (:body-object query))
        (throw+ {:type :not-found :message "no account return"}))
      (when-not (auth/authorized? cust-id acct-num subsystem)
        (throw+ {:type :not-authorized}))
      ))
  ;; (let [cust-id 28
  ;;       acct-num 1000736
  ;;       subsystem :project]
  ;;   (if-not (auth/authorized? cust-id acct-num subsystem)
  ;;     (throw+ {:type :not-authorized})))
  query)

(defn- proxy-query [query]
  (proxies/process-proxy query))

(defn- post-validate [query]
  query)

(defn add-body-object [query]
  (if-let [obj (:body-object query)]
    query
    (let [body (-> query :response :body parse-string)]
      (assoc query :body-object body))))


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

(defn default-post-authorize-fn [query account-nums]
  ;; (when (empty? account-nums)
  ;;   (utils/ppn (:body-object query))
  ;;   (throw+ {:type :not-found :message "no account return"}))
  ;; (map (fn [acct-num]
  ;;        (when-not (auth/authorized? cust-id acct-num subsystem)
  ;;          (throw+ {:type :not-authorized})))
  ;;      account-nums)
  )

(defn- post-authorize [query]
  (let [authorize-fn (:post-authorize (:query-def query))]
    (if true ;(and (perform-authorization?) authorize-fn)
      (let [q            (:query query)
            cust-id      (:customer-id q)
            subsystem    (:subsystem q)
            body         (:body-object query)
            account-nums (extract-account-relationships body)
            ]
        (println "\n\ncust, accts, subsystem:" cust-id account-nums subsystem (keys query) q)
        (println "query keys:" (keys query))
        (println "request keys:" (keys (:request query)))
        (println "\nheader keys:" (keys (:headers (:request query))))
        (println "authorization:" (:authorization (:headers (:request query))))
        (println "headers:" (:headers (:request query)))
        (println "\n")
        (if (nil? authorize-fn)
          (default-post-authorize-fn query account-nums)
          (authorize-fn query account-nums))
        )))
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
    (println "boo format-response query:" (:query query))
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
   proxy-query
   add-body-object
   post-validate
   post-authorize
   (utils/ppbl "after post-auth")
   finalize-query
   format-response))


;; (mount/start)

;; (clojure.pprint/pprint client/default-middleware)
