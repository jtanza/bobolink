(ns bobolink.handler
  (:import (java.util Base64))
  (:require [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.logger :as logger]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :as response]
            [ring.middleware.ratelimit :refer [wrap-ratelimit ip-limit]]
            [bobolink.api :as api]
            [bobolink.db :as db]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]))

(defn- decode-auth
  "Parses a Base64 encoded string into a map containing the provided `:email` and `:auth`
  values."
  [auth]
  (apply assoc {}
         (interleave [:email :auth]
                     (str/split (String. (.decode (Base64/getDecoder) auth)) #":"))))

(defn- get-auth
  "Converts a Base64 encoded Authorization header into a map containing the associated
  user and authtoken."
  [req]
  (-> (get-in req [:headers "authorization"])
      (str/split #" ")
      (last)
      (decode-auth)))

(defn- is-same-user
  "Determines whether the authenticated user present in `req` is
  equal to `user`."
  [user req]
  (let [auth-email (:email (get-auth req))]    
    (if-some [email (:email user)]
      (= email auth-email)
      (if-some [id (:id user)]
        (if-some [existing-user (db/get-user {:id id})]
          (== (Integer/parseInt id) (:id existing-user)))))))

(defn- with-version
  [route]
  (str "/v1/" route))

(defroutes protected-routes
  (POST (with-version "bookmarks") req
        (api/add-bookmarks (:email (get-auth req)) (get-in req [:body :urls])))
  (DELETE (with-version "bookmarks") req
          (api/delete-bookmarks (:email (get-auth req)) (get-in req [:body :urls])))
  (POST (with-version "bookmarks/search") req
        (api/search-bookmarks (:email (get-auth req)) (:body req)))
  (GET (with-version "users/:id/bookmarks") [id :as req]
       (if (is-same-user {:id id} req)
         (api/get-bookmarks id)
         (response/status 200)))
  (GET (with-version "users/:id") [id :as req]
       (if (is-same-user {:id id} req)
         (api/get-user {:id id})
         (response/status 200)))
  (POST (with-version "users/search") req
        (let [email (get-in req [:body :email])
              user {:email email}]
          (if (is-same-user user req)
            (api/get-user user)
            (response/status 200))))
  (route/not-found "Not Found"))

(defroutes public-routes
  (POST (with-version "users") req
        (api/add-user (:body req)))
  (GET (with-version "token") req
       (api/get-token (get-auth req))))

(defn authenticated?
  [userid token]
  (api/authenticated? userid token))

(def ^:private log-config
  (timbre/merge-config!
   {:appenders {:spit (appenders/spit-appender {:fname "./log/api.log"})}}))

(defroutes handler
  public-routes
  (wrap-basic-authentication protected-routes authenticated?))

(def app
  (-> (wrap-defaults handler api-defaults)
      (logger/wrap-log-response {:log-fn (fn [{:keys [level throwable message]}]
                                           (timbre/log level throwable message))})
      (wrap-ratelimit {:limits [(ip-limit 100)]})
      (wrap-json-body {:keywords? true})
      (wrap-json-response)))


