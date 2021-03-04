(ns bobolink.lucene-test
  (:require [bobolink.lucene :refer :all]
            [clojure.test :refer :all])
  (:import (org.apache.lucene.store RAMDirectory)))

(deftest lucene
  (testing "disk-index"
    (let [index (disk-index (clojure.java.io/file "/tmp/bobo-index"))]
      (is (not (nil? index)))))

  (testing "add"
    (let [index (RAMDirectory.)]
      (add! index [{:content "hi there" :url "http://foo.bar"}])
      (is (== 1 (count (search index {:query "hi"}))))
      (add! index (take 5 (repeat {:content "hi there" :url "http://foo.bar"})))
      ;; never add dupes (uniq on url)
      (is (== 1 (count (search index {:query "hi"}))))))

  (testing "search"
    (let [index (RAMDirectory.)]
      (add! index [{:content "hi there" :url "http://foo.bar"}])
      (add! index [{:content "this is some text" :url "http://foo.baz"}])
      (is (== 1 (count (search index {:query "hi"}))))
      (is (== 2 (count (search index {:query "hi OR some"}))))
      (is (== 1 (count (search index {:query "content:hi"}))))
      (is (== 1 (count (search index {:query "foo.baz" :field "url"}))))
      (is (== 2 (count (search index {:query "foo.*" :field "url"}))))))
  
  (testing "delete"
    (let [index (RAMDirectory.)]
      (add! index [{:content "hi there" :url "http://foo.bar"}])
      (add! index [{:content "hi this is some text" :url "http://foo.baz"}])
      (is (== 2 (count (search index {:query "hi"}))))
      (delete! index [{:id "http://foo.bar"}])     
      (is (== 1 (count (search index {:query "hi"})))))))

