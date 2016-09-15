(ns rampart.authorization
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [rampart.config :refer [env]]

            ))


(defn decipher-webtoken [secret token]
  (jwt/unsign token secret))

(defn webtoken->customer-id
  ([token] (webtoken->customer-id (env :secret) token))
  ([secret token] (:customer_id (jwt/unsign token secret))))

;; "TODO: if error here, throw 4??"
(defn request->customer-id
  "found from the webtoken in Authoriation line of header"
  [req]
  (when-let [auth (:authorization (:headers req))]
    (let [token (last (str/split auth #" "))]
      (webtoken->customer-id token))))

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
  {:all>      [:outbound> :credit :financial :authorization]
   :outbound> [:order> :chat]
   :ordering> [:price]
   :project>  [:order> :project]
   })

(def permissions
  {:new   [:read :create]
   :edit  [:read :create :update]
   :admin [:read :create :update :delete]})

(defn customer-subsystems [id]
  (let [grants (db/get-customer-subsystems {:id id})
        resources (map (comp keyword :resource) grants)]
    (into {} (map #(vector % [:all>]) resources)))
  )
;; (customer-account-subsystems {:id 28})

(defn customer-account-subsystems [id]
  (let [grants (db/get-customer-account-subsystems {:id id})
        accounts (map :account_number grants)]
    (into {} (map #(vector (Integer. %) [:all>]) accounts)))
  )
;; (customer-subsystems {:id 28})

(defn permissions-for [customer-id account-num subsystem]
  (let [global (customer-subsystems customer-id)]))

;; (db/get-customer {:id 28})
;; (db/get-customer-subsystems {:id 28})
;; (db/get-customer-account-subsystems {:id 28})

;; NOTE: webtoken->customer-id should always be called (directly or indirectly)
;;       to ensure the token has not expired.

;; (mount/start)
;; ()
