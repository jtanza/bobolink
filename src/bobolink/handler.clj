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

(defn- get-same-user
  "Attempts to return an email value from a user's request iff it matches
  their provided authentication, nil otherwise."
  [req]
  (let [email (:email (get-auth req))]
    (if (= email (get-in req [:params :email]))
      email)))

(defroutes protected-routes
  (GET "/users/:userid/bookmarks" [userid]
       (api/get-bookmarks userid))
  (POST "/bookmarks" req
       (api/add-bookmarks (:email (get-auth req)) (get-in req [:body :urls])))
  (DELETE "/bookmarks" req
          (api/delete-bookmarks (:email (get-auth req)) (get-in req [:body :urls])))
  (POST "/bookmarks/search" req
        (api/search-bookmarks (:email (get-auth req)) (:body req)))
  (POST "/users/search" req
       (if-let [email (get-same-user req)] 
         (api/get-user {:email email})
         (response/not-found "Not Found")))
  (GET "/users/:id" [id]
       (api/get-user {:id id}))
  (route/not-found "Not Found"))

(defroutes public-routes
  ;; TODO handle already created users
  (POST "/users" req
        (api/add-user (:body req)))
  (GET "/token" req
       (api/get-token (get-auth req))))

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
