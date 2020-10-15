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

(defn mmap [a b]
  (map b a))

(defn handlers [address id]
  (let [config (config address)]
    (->> (-> config
             :apps
             :http
             :servers
             id
             :routes)
         (map :handle)
         (flatten)
         (into []))))

(comment
  (pprint (handlers (connect "http://localhost:2019") :my-id)))

(defn routes [address id]
  (let [config (config address)]
    (-> config
        :apps
        :http
        :servers
        id
        :routes)))

(comment
  (pprint (routes (connect "http://localhost:2019") :my-id)))

(def example-routes
  [{:handle [{:handler "file_server", :root "/tmp/cawdytest"}],
    :match [{:host ["cawdy.127.0.0.1.xip.io"]}]}
   {:handle [{:handler "file_server", :root "/tmp/cawdytest2"}],
    :match [{:host ["cawdy2.127.0.0.1.xip.io"]}]}])

(defn has-route-with-host? [routes host]
  (let [all-matchers (map #(-> % :match first) routes)
        all-hosts (map #(-> % :host first) all-matchers)]
    (boolean
      (some #(= host %) all-hosts))))

(defn replace-route-with-host [routes host with]
  (into []
    (if (has-route-with-host? routes host)
      (map (fn [route]
             (if (= (-> route :match first :host first)
                    host)
               with
               route))
           routes)
      (conj routes with))))

(comment
  ;; Overwriting
  (replace-route-with-host example-routes
                           "cawdy.127.0.0.1.xip.io"
                           {:yeah 'boi})
  ;; no previous
  (replace-route-with-host []
                           "cawdy.127.0.0.1.xip.io"
                           {:yeah 'boi})

  ;; no match adds
  (replace-route-with-host example-routes
                           "cawdy3.127.0.0.1.xip.io"
                           {:yeah 'boi}))

(defn clean-config [conn]
  (-> (http/delete (str (:address conn) "/config/")
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
  (def example-routes [{:handle [{:handler "file_server",
                                  :root "/tmp/cawdytest"}]
                        :match [{:host ["cawdy.xip.io"]}]}
                       {:handle [{:handler "static_Response",
                                  :body "something"}]
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

(defn create-server [conn id listen]
  (let [cfg (config conn)
        server {:listen [listen]
                :automatic_https {:disable true}
                :routes []}
        new-cfg (assoc-in cfg
                          [:apps :http :servers id]
                          server)]
    (save-config conn new-cfg)
    new-cfg))

(defn static-route [{:keys [body]}]
  {:handler "static_response"
   :body body})

(defn files-route [{:keys [root]}]
  {:handler "file_server"
   :root root})

(def route-types
  {:static static-route
   :files files-route})

(defn add-route [conn id host type arg]
  (when (nil? (get server-types type))
    (throw (Exception. (format "Couldn't find server of type %s" type))))
  (let [cfg (config conn)
        route-fn (get route-types type)
        route-res (route-fn arg)

        routes (routes conn id)
        _ (println "current routes")
        _ (pprint routes)

        new-routes (replace-route-with-host
                     routes
                     host
                     route-res)

        _ (println "new routes")
        _ (pprint new-routes)

        handler (merge {:match [{:host [host]}]
                        :handle new-routes})
        new-cfg (assoc-in cfg
                          [:apps :http :servers id :routes]
                          new-routes)]
    (println "saving cfg")
    (pprint new-cfg)
    (save-config conn new-cfg)
    new-cfg))

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
