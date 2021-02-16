(ns bobolink.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [bobolink.handler :refer :all]))

(deftest test-app
  (comment (testing "main route"
     (let [response (app (mock/request :get "/"))]
       (is (= (:status response) 200))
       (is (= (:body response) "Hello World")))))

  (comment (testing "not-found route"
     (let [response (app (mock/request :get "/invalid"))]
       (is (= (:status response) 404))))))
