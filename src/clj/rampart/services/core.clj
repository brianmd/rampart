(ns rampart.services.core
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [rampart.config :refer [env]]
            ))

;; TODO: should not use request
(defn reconstitute-uri [base-url request]
  (if (nil? base-url)
    (throw "base-url must be set in reconstitute-uri. Perhaps need to call mount/start to load the environment?"))
  (let [query-str (if-let [q (:query-string request)]
                    (str "?" q)
                    "")
        ]
    (str base-url (:uri request) query-str)
    ))

(def debug-options
  (atom {:debug false
         :debug-body false}))
  ;; {:debug true
  ;;  :debug-body true})

