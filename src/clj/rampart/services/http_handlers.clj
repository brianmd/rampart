(ns rampart.services.http-handlers
  (:import [java.net URI])
  (:require ;[mishmash.event-logger :as event-logger]
            ;; [cheshire.core :as json]
            [clj-http.client :as client]))

(defonce base-event (atom {}))

;; (defn wrap-log-request-duration
;;   [handler]
;;   (fn [req]
;;     (let [response (atom nil)
;;           error (atom nil)
;;           start-time (System/nanoTime)
;;           ]
;;       (try
;;         (reset! response (handler req))
;;         (catch Throwable e
;;           (reset! error e)))
;;       (let [duration (/ (- (System/nanoTime) start-time) 1e6)]
;;         (event-logger/with-merged-event @base-event
;;           (if @error
;;             (event-logger/log {:service "http.duration" :state "critial" :metric duration})
;;             (event-logger/log {:service "http.duration" :state "ok" :metric duration})
;;             ))
;;         (if @error
;;           (throw @error)
;;           @response)))))

(defn prepare-cookies
    "Removes the :domain and :secure keys and converts the :expires key (a Date)
    to a string in the ring response map resp. Returns resp with cookies properly
    munged."
  [resp]
  (let [prepare #(-> (update-in % [1 :expires] str)
                     (update-in [1] dissoc :domain :secure))]
    (assoc resp :cookies (into {} (map prepare (:cookies resp))))))

(defn slurp-binary
    "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.read rdr buf)
      buf)))

(defn proxy-request
  [from-path to-base-uri request & [http-opts]]
  (println "\noriginal request:")
  (clojure.pprint/pprint request)
  (clojure.pprint/pprint http-opts)
  (let [rmt-full   (URI. (str to-base-uri "/"))
        rmt-path   (URI. (.getScheme    rmt-full)
                         (.getAuthority rmt-full)
                         (.getPath      rmt-full) nil nil)
        lcl-path   (URI. (subs (:uri request) (.length from-path)))
        to-uri (.resolve rmt-path lcl-path) ]
    (println "to-uri:" to-uri)
    (let [new-request
          (merge {:method (:request-method request)
                  :url (str to-uri "?" (:query-string request))
                  :headers (dissoc (:headers request) "host" "content-length")
                  :body (if-let [len (get-in request [:headers "content-length"])]
                          (slurp-binary (:body request) (Integer/parseInt len)))
                  ;; :body (json/generate-string (:body request))
                  :body-params (:body-params request)
                  ;; :body "abc"
                  :follow-redirects true
                  :throw-exceptions false
                  :as :stream} http-opts)
          ]
      (println "\nnew request:")
      (prn new-request)
      (println "\n")
      (client/request new-request))))
;; (let [response (proxy-request "/" "https://www.summit.com/" ; "https://www.google.com/"
;;                               {:uri "/"
;;                                :request-method :get
;;                                :query-string ""})]
;;   (clojure.pprint/pprint response)
;;   (println "\n\nbody:")
;;   (println (slurp (:body response))))

;; (let [response (proxy-request "/" "https://www.summit.com/" ; "https://www.google.com/"
;;                               {:uri "/api/v2/projects/3/releases"
;;                                :request-method :get
;;                                ;; :query-string ""
;;                                })]
;;   (clojure.pprint/pprint response)
;;   (println "\n\nbody:")
;;   (println (slurp (:body response))))
;; (let [response (proxy-request "/" "http://10.9.0.124:3005/"
;;                               {:uri "/api/v2/projects/3/releases"
;;                                :request-method :get
;;                                ;; :query-string ""
;;                                })]
;;   (clojure.pprint/pprint response)
;;   (println "\n\nbody:")
;;   (println (slurp (:body response))))

;; (let [response (proxy-request "/" "http://10.9.0.124:3005/"
;;                               {:uri "/api/v2/releases"
;;                                :body {:attributes {:name "test 1"}
;;                                       :relationships {:project {:data {:id 3}}
;;                                                       :requestor {:data {:id 123}}}}
;;                                :request-method :post
;;                                ;; :query-string ""
;;                                })]
;;   (clojure.pprint/pprint response)
;;   (println "\n\nbody:")
;;   (println (slurp (:body response))))
;; (let [response (proxy-request "/" "http://10.9.0.124:3005/"
;;                               {:uri "/api/v2/release-line-items"
;;                                :body {:attributes {:quantity 3}
;;                                       :relationships {:release {:data {:id "Release350657409571111"}}
;;                                                       :project-line-item {:data {:id "1234"}}}}
;;                                :request-method :post
;;                                ;; :query-string ""
;;                                })]
;;   (clojure.pprint/pprint response)
;;   (println "\n\nbody:")
;;   (println (slurp (:body response))))

;; (let [response (proxy-request "/" "https://www.summit.com/" ; "https://www.google.com/"
;;                               {:uri "/api/v2/projects/3/releases"
;;                                :body {:name "test 1"}
;;                                :request-method :post
;;                                ;; :query-string ""
;;                                })]
;;   (clojure.pprint/pprint response)
;;   (println "\n\nbody:")
;;   (println (slurp (:body response))))
