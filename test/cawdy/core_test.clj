(ns cawdy.core-test
  (:require
    [clojure.test :refer :all]
    [clojure.pprint :refer [pprint]]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.java.io :as io]
    [cawdy.core :as cawdy]))

(def conn (cawdy/connect "http://localhost:2019"))

;; cawdy API - Requires running caddy instance with Admin API on localhost:2019
;; Tested with Caddy v2.2.0

(defn get-config []
  (cawdy/config conn))

(defn get-handlers [id]
  (cawdy/handlers (:address conn) id))

(defn add-simple-response [id]
  (cawdy/add-server
    (:address conn)
    id
    :static
    {:body "hello"}))

(defn add-static-server
  ([id directory]
   (cawdy/add-server
     (:address conn)
     id
     :files
     {:directory directory}))
  ([id directory listen]
   (cawdy/add-server
     (:address conn)
     id
     :files
     {:directory directory
      :listen listen}))
  ([id directory listen host]
   (cawdy/add-server
     (:address conn)
     id
     :files
     {:directory directory
      :listen listen
      :host host})))

(defn clean []
  (cawdy/clean-config (:address conn)))

;; Test utils
(defn http-is [url should-be]
  (is (= should-be
         (:body (http/get url {:content-type :json
                               :retry-handler (fn [ex try-count http-context]
                                                (println "Got:" ex)
                                                (Thread/sleep 1000)
                                                (if (> try-count 4) false true))})))))

(defn create-directory [path file-content]
  (let [file-path (str path "/file")]
    (io/make-parents file-path)
    (spit (io/file file-path) file-content)))

(comment

  (def conn (cawdy/connect "localhost:2019"))

  (-> conn
      (create-server ":443")
      (add-route server "localhost" "/tmp/cawdytest")
      (remove-route server "localhost")
      (delete-server ":443")))


(deftest cawdy-tests
  (testing "setting config"
    (clean)
    (is (= {} (get-config)))
    (let [server (cawdy/create-server conn "2015")]
      (cawdy/add-route conn server "localhost" :static "hello")
      (is (= (get-config)
             {:apps {:http {:servers {:my-id {:listen [":2015"],
                                              :automatic_https {:disable true}
                                              :routes [{:match [{:host ["localhost"]}]
                                                        :handle [{:body "hello",
                                                                  :handler "static_response"}]}]}}}}}))))
  (testing "getting handlers"
    (clean)
    (add-simple-response :my-id)
    (is (= (get-handlers :my-id)
           [{:body "hello",
             :handler "static_response"}])))

  (testing "Setting simple response"
    (clean)
    (add-simple-response :my-id)
    (http-is "http://localhost:2015" "hello"))

  (testing "Add files server"
    (clean)
    (create-directory "/tmp/cawdytest" "hello there")
    (add-static-server :my-id "/tmp/cawdytest")
    (http-is "http://localhost:2015/file" "hello there"))

  (testing "Files server with different listen address"
    (clean)
    (create-directory "/tmp/cawdytest" "hello there")
    (add-static-server :my-id "/tmp/cawdytest" ":2016")
    (http-is "http://localhost:2016/file" "hello there"))

  (testing "Listen to domain"
    (clean)
    (create-directory "/tmp/cawdytest" "hello from domain")
    (add-static-server :my-id "/tmp/cawdytest" ":2016" "cawdy.127.0.0.1.xip.io")
    (http-is "http://cawdy.127.0.0.1.xip.io:2016/file" "hello from domain"))

  (testing "Two domains at the same time"
    (clean)
    (create-directory "/tmp/cawdytest" "hello from domain")
    (create-directory "/tmp/cawdytest2" "hello from domain2")

    (add-static-server :my-id "/tmp/cawdytest" ":2016" "cawdy.127.0.0.1.xip.io")
    (add-static-server :my-other-id "/tmp/cawdytest2" ":2016" "cawdy2.127.0.0.1.xip.io")

    (http-is "http://cawdy.127.0.0.1.xip.io:2016/file" "hello from domain")
    (http-is "http://cawdy2.127.0.0.1.xip.io:2016/file" "hello from domain2"))

  (testing "Overwriting domain"
    (clean)
    (create-directory "/tmp/cawdytest" "hello from domain")
    (create-directory "/tmp/cawdytest2" "hello from domain2")

    (add-static-server :my-id "/tmp/cawdytest" "localhost:2016" "cawdy.127.0.0.1.xip.io")
    (http-is "http://cawdy.127.0.0.1.xip.io:2016/file" "hello from domain")

    (add-static-server :my-id "/tmp/cawdytest2" "localhost:2016" "cawdy.127.0.0.1.xip.io")
    (http-is "http://cawdy.127.0.0.1.xip.io:2016/file" "hello from domain2")))
