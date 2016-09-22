(ns rampart.authorization
  (:require [clojure.string :as str]
            [clojure.set :as set]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [rampart.config :refer [env]]

            [clojure.set :as set]))


(defn decipher-webtoken [secret token]
  (jwt/unsign token secret))

(defn webtoken->customer-id
  ([token] (webtoken->customer-id (env :secret) token))
  ([secret token] (:customer_id (jwt/unsign token secret))))

;; "TODO: if error here, throw 4??"
(defn request->customer-id
  "found from the webtoken in Authoriation line of header"
  [req]
  (if (= (env :pw) (-> req :params :env :pw))
    (-> req :params :env :customer-id)
    (when-let [auth (get-in req [:headers "authorization"])]
      (let [token (last (str/split auth #" "))]
        (webtoken->customer-id token)))))

(defn bh-webtoken
  "returns webtoken after logging into bh"
  [email pw]
  (let [cred
        {:data
         {:type "tokens"
          :attributes {:email email :password pw}}}
        params
        {:body         (generate-string cred)
         :content-type "application/vnd.api+json"
         :accept       :json}
        result (client/post
                "https://www.summit.com/api/tokens"
                params)
        body (parse-string (:body result))
        ]
    (((body "data") "attributes") "token")
    ))

(defn bh-customer-id
  "returns customer-id after logging into bh"
  [email pw]
  (webtoken->customer-id (bh-webtoken email pw)))

;; rampart will not have access to bh's database,
;;         so we should not provide functions grabbing data from it.
;; (defn bh-login
;;   "returns customer record after logging into bh"
;;   [email pw]
;;   (let [id (bh-customer-id email pw)]
;;     (db/get-customer {:id id})))

(def subsystems
  {
   :all-flat  [:order :price :project :chat-admin :credit :financial :authorization :admin]
   :all-account-flat [:order :price :project :credit :financial :authorization :admin]
   :credit-flat  [:price :credit :financial]
   :all>      [:outbound> :credit :financial :authorization]
   :outbound> [:order> :chat-admin]
   :order> [:order :price]
   :project>  [:order :project]
   })

(def permissions
  {:new   [:read :create]
   :edit  [:read :create :update]
   :admin [:read :create :update :delete]})

;; (db/get-customer {:id 28})
;; (db/get-customer-subsystems {:id 28})
;; (db/get-customer-account-subsystems {:id 28})

(defn customer-subsystems [id]
  (let [grants (db/get-customer-subsystems {:id id})
        resources (map (comp keyword :resource) grants)
        all-subsystems (:all-flat subsystems)
        credit-subsystems (:credit-flat subsystems)
        ]
    ;; (into {} (map #(vector % (subsystems (keyword (str (name %) "-flat")))) resources)))
    (set (apply concat (map #(subsystems (keyword (str (name %) "-flat"))) resources))))
  )
;; (customer-subsystems 28)

(defn all-customer-account-subsystems [id]
  (let [grants (db/get-customer-account-subsystems {:id id})
        accounts (map :account_number grants)
        all-subsystems (:all-account-flat subsystems)
        ]
    (into {} (map #(vector (Integer. %) (set all-subsystems)) accounts)))
  )
;; (customer-subsystems 28)
;; (customer-subsystems 4444)
;; (all-customer-account-subsystems 28)

(defn customer-account-specific-subsystems [customer-id account-num]
  (let [all (all-customer-account-subsystems customer-id)
        acct-subsystems (all account-num)]
    (if acct-subsystems acct-subsystems #{})))

(defn customer-account-subsystems [customer-id account-num]
  (set/union (customer-subsystems customer-id)
             (customer-account-specific-subsystems customer-id account-num)))

;; (customer-account-subsystems 28 1000736)
;; (customer-account-subsystems 28 1002225)
;; (customer-account-subsystems 28 1002225)
;; (customer-account-subsystems 42 1023292)

(defn authorized? [customer-id account-num subsystem]
  (contains? (customer-account-subsystems customer-id account-num) subsystem))
;; (authorized? 28 1000736 :project)  ;; true
;; (authorized? 28 1000736 :projectttt)  ;; false
;; (authorized? 28 nil :project)  ;; true
;; (authorized? 39 1021734 :project)  ;; true
;; (authorized? 39 nil :project)  ;; false
;; (authorized? 39 1000736 :project)  ;; false
