(ns bobolink.api-test
  (:use bobolink.api
        clojure.test))

(deftest api
  (testing "add-user"
    (is (= {:status 400, :headers {}, :body "email or password missing"}
           (add-user {:email "foo@bar.com"})))
    (is (= {:status 400, :headers {}, :body "email or password missing"}
           (add-user {:password "foo"})))
    (is (= {:status 400, :headers {}, :body "invalid email address"}
           (add-user {:email "foo" :password "foo"})))
    (is (= {:status 400, :headers {}, :body "password must be minimum eight characters, have at least one uppercase letter, one lowercase letter, and one number"}
           (add-user {:email "f@b.com" :password "foo"})))))

