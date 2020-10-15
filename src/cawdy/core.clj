(ns cawdy.core
  (:require
    [clojure.pprint :refer [pprint]]
    [cheshire.core :as json]
    [clj-http.client :as http]))

(defn connect [address]
  {:address address})

(defn config [conn]
  (-> (http/get (str (:address conn) "/config")
                {:content-type :json})
      :body
      (json/parse-string true)
      (or {})))

(defn handlers [address id]
  (let [config (config address)]
    (-> config
        :apps
        :http
        :servers
        id
        :routes
        first
        :handle)))

(defn routes [address id]
  (let [config (config address)]
    (-> config
        :apps
        :http
        :servers
        id
        :routes)))

(defn clean-config [address]
  (-> (http/delete (str address "/config/")
                   {:content-type :json})
      :body))

(defn static-server [{:keys [listen body host]
                       :or {listen ":2015"
                            host "localhost"}}]
  {:listen [listen]
   :automatic_https {:disable true}
   :routes [{:match [{:host [host]}]
             :handle [{:handler "static_response"
                       :body body}]}]})

(defn files-server [{:keys [listen directory host]
                      :or {listen ":2015"
                           host "localhost"}}]
  {:listen [listen]
   :automatic_https {:disable true}
   :routes [{:match [{:host [host]}]
             :handle [{:handler "file_server"
                       :root directory}]}]})

(def server-types
  {:static static-server
   :files files-server})

(defn has-route-with-host? [routes host]
  (let [all-matchers (map #(-> % :match first) routes)
        all-hosts (map #(-> % :host first) all-matchers)]
    (boolean
      (some #(= host %) all-hosts))))

(comment
  (def example-routes [{:handle [{:handler "file_server", :root "/tmp/cawdytest"}]
                        :match [{:host ["cawdy.xip.io"]}]}
                       {:handle [{:handler "static_Response", :body "something"}]
                        :match [{:host ["cawdy2.xip.io"]}]}])

  (has-route-with-host? example-routes "cawdy.xip.io")
  ;; => true
  (has-route-with-host? example-routes "cawdy2.xip.io")
  ;; => true
  (has-route-with-host? example-routes "cawdy3.xip.io"))
  ;; => false

(defn save-config [conn cfg]
  (http/post (str (:address conn) "/load")
             {:content-type :json
              :body (json/generate-string cfg)}))

(defn create-server [conn listen]
  (let [cfg (config conn)
        new-cfg (assoc-in cfg
                          [:apps :http :servers listen]
                          {})]
    (save-config conn new-cfg)
    {:listen [listen]
     :automatic_https {:disable true}
     :routes []}))

(defn add-route [conn server host type arg]
  (when (nil? (get server-types type))
    (throw (Exception. (format "Couldn't find server of type %s" type))))
  (let [cfg (config conn)]
    (update server :routes conj {:handle []})))

(defn add-server [address id type args]
  (when (nil? (get server-types type))
    (throw (Exception. (format "Couldn't find server of type %s" type))))
  (let [cfg (config address)
        server-fn (get server-types type)
        server-res (server-fn args)
        existing-routes (routes address id)
        _ (println "existing-routes")
        _ (pprint existing-routes)
        new-cfg (assoc-in cfg
                          [:apps :http :servers id]
                          server-res)]
    (http/post (str address "/load")
               {:content-type :json
                :body (json/generate-string new-cfg)})))
