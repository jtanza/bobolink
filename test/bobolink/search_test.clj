(ns bobolink.search-test
  (:use bobolink.search
        clojure.test)
  (:require [amazonica.aws.s3 :as s3]))

(deftest search

  (testing "gen-bookmark"
    (let [bookmark (gen-bookmark {:id 1 :email "foo@bar" :password "secret"} "https://en.wikipedia.org/wiki/1355")]
      (is (== 1 (:userid bookmark)))
      (is (nil? (:password bookmark)))
      (is (nil? (:email bookmark)))
      (is (== "https://en.wikipedia.org/wiki/1355" (:url bookmark)))
      (is (not (nil? (:content bookmark))))))

  (testing "save-bookmarks"
    (let [user {:id 101}
          bookmark (gen-bookmark user "https://en.wikipedia.org/wiki/Byzantine_calendar")
          index (save-bookmarks user [bookmark])]
      (is (not (nil? index))) ;; index added to cache
      (is (== 1 (count (search-bookmarks user "calendar")))) ;; bookmark indexed
      (is (not (nil? (s3/get-object-metadata {:bucket-name "bobo-index" :key "101-index.zip"})))))) ;; index synced to s3

  (testing "search-bookmarks"
    (let [user {:id 101}
          bookmark (gen-bookmark user "https://en.wikipedia.org/wiki/Guatemalan_Revolution")
          index (save-bookmarks user [bookmark])]
      (is (not (nil? index)))
      (is (== 1 (count search-bookmarks user "revolution")))
      ;; test highlights
      )))


