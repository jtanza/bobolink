(ns bobolink.handler
  (:import (java.util Base64))
  (:require [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :as response]
            [bobolink.api :as api]))

(defn- decode-auth
  [auth]
  (apply assoc {}
         (interleave [:email :auth]
                     (str/split (String. (.decode (Base64/getDecoder) auth)) #":"))))

(defn- auth-from-req
  [req]
  (-> (get-in req [:headers "authorization"])
      (str/split #" ")
      (last)
      (decode-auth)))

(defroutes protected-routes
  (GET "/bookmarks" [userid]
       (api/get-bookmarks userid))
  (PUT "/bookmarks" req
       (api/add-bookmarks (:email (auth-from-req req)) (get-in req [:body :urls])))
  (DELETE "/bookmarks" req)
  (POST "/bookmarks/search" req)
  (GET "/users" req
       (let [email (:email (auth-from-req req))]
         (if (= email (get-in req [:params :email]))
           (api/get-user {:email email})
           (response/not-found "Not Found"))))
  (GET "/users/:id" [id]
       (api/get-user {:id id}))
  (route/not-found "Not Found"))

(defroutes public-routes
  (POST "/users" req
        (api/add-user (:body req)))
  (GET "/token" req
       (api/get-token (auth-from-req req))))

(defn authenticated?
  [userid token]
  (api/authenticated? userid token))

(defroutes handler
  public-routes
  (wrap-basic-authentication protected-routes authenticated?))

(def app
  (-> (wrap-defaults handler api-defaults)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)))
