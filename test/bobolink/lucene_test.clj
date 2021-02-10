(ns bobolink.lucene-test
  (:use bobolink.lucene
        clojure.test)
  (:import (org.apache.lucene.store RAMDirectory)))

(deftest lucene
  
  (testing "disk-index"
    (let [index (disk-index (clojure.java.io/file "/tmp/bobo-index"))]
      (is (not (nil? index)))))

  (testing "add"
    (let [index (RAMDirectory.)]
      (do (add! index [{:content "hi there" :url "http://foo.bar"}]))
      (is (== 1 (count (search index "hi"))))))

  (testing "search"
    (let [index (RAMDirectory.)]
      (do
        (add! index [{:content "hi there" :url "http://foo.bar"}])
        (add! index [{:content "this is some text" :url "http://foo.baz"}]))
      (is (== 1 (count (search index "hi"))))
      (is (== 1 (count (search index "http://foo.baz" "url"))))
      (is (== 1 (count (search index "foo.bar" "url"))))
      (is (== 2 (count (search index "foo*" "url"))))
      (is (== 2 (count (search index "hi OR some"))))
      (is (== 1 (count (search index "content:hi"))))
      (is (== 2 (count (search index "url:foo.b*"))))))

  (testing "delete"
    (let [index (RAMDirectory.)]
      (do
        (add! index [{:content "hi there" :url "http://foo.bar"}])
        (add! index [{:content "this is some text" :url "http://foo.baz"}]))
      (is (== 2 (count (search index "foo*" "url"))))
      (do
        (delete! index [{:content "hi"}]))
      (is (== 1 (count (search index "foo*" "url"))))
      (do
        (delete! index [{:url "foo.baz"}]))
      (is (== 0 (count (search index "foo*" "url"))))))
  
  )
