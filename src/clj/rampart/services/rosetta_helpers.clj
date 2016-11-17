(ns rampart.services.rosetta-helpers
  (:require ;[clojure.string :as str]
 
            ;; [mount.core :as mount]
            ;; [rampart.db.core :as db]

            ;; [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            ;; [clj-http.client :as client]
            ;; [clj-http.conn-mgr :as conn-mgr]
            ;; [slingshot.slingshot :refer [throw+]]
            ;; [rampart.config :refer [env]]

            [summit.utils.core :as utils]
            [rampart.web-query :as webquery]
            [rampart.process-query :as processquery]
            ;; [rampart.authorization :as auth]
            ;; [rampart.services :as services]

            ;; [rampart.services.rosetta :as rosetta]
            ))

(defn default-server []
  (processquery/process
   (webquery/make-query-request :status
                                :default-server
                                {:uri (str "/api/v2/default-server")
                                 ;; :request-method :get
                                 ;; :params {}
                                 })))
;; (def p (default-server))
;; (:result (default-server))
;; (-> p :response :body)
;; (:result p)


(defn get-projects [server account-num]
  (processquery/process
   (webquery/make-query-request :project
                                :projects
                                {:uri (str "/api/v2/projects?filter[account]=" account-num "&env[server]=" (utils/->str server))
                                 :request-method :get
                                 :params {:account account-num}
                                 })))
;; (first ((:result (get-projects :prd "1037657")) "data"))
;; (def p (get-projects :prd "1037657"))
;; (-> p :result keys)  ;; => ("data" "meta")
;; (first ((:result p) "data"))
;; (first ((:result p) "meta"))

(defn get-project [server id]
  (processquery/process
   (webquery/make-query-request :project
                                :project
                                {:uri (str "/api/v2/projects/" id "?env[server]=" (utils/->str server))
                                 :request-method :get
                                 :params {:id id}
                                 })))
;; (def p (get-project :prd 3))
;; (-> p :result keys)  ;; => ("status-lines" "messages" "delivery-attr-defs" "order-attr-defs" "line-item-attr-defs" "meta")
;; (first ((:result p) "status-lines"))
;; (first ((:result p) "meta"))

(defn get-project-spreadsheet-data [server id]
  (processquery/process
   (webquery/make-query-request :project
                                :project-spreadsheet-data
                                {:uri (str "/api/v2/project-spreadsheet-data/" id "?env[server]=" (utils/->str server))
                                 :request-method :get
                                 :params {:id id}
                                 })))
;; (def p (get-project-spreadsheet-data :prd 3))
;; (-> p :result keys)   ;; => ("account-num" "project-id" "project-name" "headers" "data")
;; ((:result p) "account-num")
;; ((:result p) "headers")
;; (first ((:result p) "data"))

