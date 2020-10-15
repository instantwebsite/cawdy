(ns cawdy.core
  (:require
    [clojure.pprint :refer [pprint]]
    [cheshire.core :as json]
    [clj-http.client :as http]))

(defn config [address]
  (-> (http/get (str address "/config")
                {:content-type :json})
      :body
      (json/parse-string true)
      (or {})))

(defn clean-config [address]
  (-> (http/delete (str address "/config/")
                   {:content-type :json})
      :body))

(defn static-handler [{:keys [listen body host]
                       :or {listen ":2015"
                            host "localhost"}}]
  {:listen [listen]
   :routes [{:handle [{:handler "static_response"
                       :body body}]}]})

(defn files-handler [{:keys [listen directory host]
                      :or {listen ":2015"
                           host "localhost"}}]
  {:listen [listen]
   :routes [{:handle [{:handler "file_server"
                       :host host
                       :root directory}]}]})

(def handler-types
  {:static static-handler
   :files files-handler})

(defn add-server [address id type args]
  (when (nil? (get handler-types type))
    (throw (Exception. (format "Couldn't find handler of type %s" type))))
  (let [cfg (config address)
        handler-fn (get handler-types type)
        handler-res (handler-fn args)
        new-cfg (assoc-in cfg
                          [:apps :http :servers id]
                          handler-res)]
    (http/post (str address "/load")
               {:content-type :json
                :body (json/generate-string new-cfg)})))
