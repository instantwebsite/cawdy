(ns cawdy.core-test
  (:require
    [clojure.test :refer :all]
    [clojure.pprint :refer [pprint]]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.java.io :as io]
    [cawdy.core :as cawdy]))

(def api-uri "http://localhost:2019")

;; cawdy API - Requires running caddy instance with Admin API on localhost:2019
;; Tested with Caddy v2.2.0

(defn get-config []
  (cawdy/config api-uri))

(defn add-simple-response [id]
  (cawdy/add-server
    api-uri
    id
    :static
    {:body "hello"}))

(defn add-static-server
  ([id directory]
   (cawdy/add-server
     api-uri
     id
     :files
     {:directory directory}))
  ([id directory listen]
   (cawdy/add-server
     api-uri
     id
     :files
     {:directory directory
      :listen listen}))
  ([id directory listen host]
   (cawdy/add-server
     api-uri
     id
     :files
     {:directory directory
      :listen listen
      :host host})))

(defn clean []
  (cawdy/clean-config api-uri))

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

(deftest cawdy-tests
  (testing "setting config"
    (clean)
    (is (= {} (get-config)))
    (add-simple-response :my-id)
    (is (= (get-config)
           {:apps {:http {:servers {:my-id {:listen [":2015"],
                                            :routes [{:handle [{:body "hello",
                                                                :handler "static_response"}]}]}}}}})))
  (testing "Setting simple response"
    (clean)
    (add-simple-response :my-id)
    (http-is "http://localhost:2015" "hello"))

  (testing "Add files server"
    (clean)
    (create-directory "/tmp/cawdytest" "hello there")
    (add-static-server :my-id "/tmp/cawdytest")
    (pprint (get-config))
    (http-is "http://localhost:2015/file" "hello there"))

  (testing "Files server with different listen address"
    (clean)
    (create-directory "/tmp/cawdytest" "hello there")
    (add-static-server :my-id "/tmp/cawdytest" ":2016")
    (http-is "http://localhost:2016/file" "hello there"))

  (testing "Listen to domain"
    (clean)
    (create-directory "/tmp/cawdytest" "hello from domain")
    (add-static-server :my-id "/tmp/cawdytest" "cawdy.127.0.0.1.xip.io:2016")
    (http-is "http://cawdy.127.0.0.1.xip.io:2016/file" "hello from domain"))

  (testing "Two domains at the same time"
    (clean)
    (create-directory "/tmp/cawdytest" "hello from domain")
    (create-directory "/tmp/cawdytest2" "hello from domain2")

    (add-static-server :my-id "/tmp/cawdytest" "localhost:2016" "cawdy.127.0.0.1.xip.io")
    (add-static-server :my-id "/tmp/cawdytest2" "localhost:2016" "cawdy2.127.0.0.1.xip.io")

    (http-is "http://cawdy.127.0.0.1.xip.io:2016/file" "hello from domain")
    (http-is "http://cawdy2.127.0.0.1.xip.io:2016/file" "hello from domain2")))
