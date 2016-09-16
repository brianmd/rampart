(ns rampart.proxies.core
  (:require [clojure.string :as str]

            [mount.core :as mount]
            [rampart.db.core :as db]

            [buddy.sign.jwt :as jwt]
            [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [rampart.config :refer [env]]
            ))

(defn reconstitute-uri [base-url request]
  (let [query-str (if-let [q (:query-string request)]
                    (str "?" q)
                    "")
        ]
    (str base-url (:uri request) query-str)
    ))

(def debug-options
  {:debug false
   :debug-body false})
  ;; {:debug true
  ;;  :debug-body true})

