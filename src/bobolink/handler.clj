(ns bobolink.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :as response]
            [bobolink.api :as api]))

(defroutes app-routes
  (GET "/bookmarks" [userid]
       (api/get-bookmarks userid))
  (PUT "/bookmarks" req
       (api/add-bookmarks (:body req)))
  (DELETE "/bookmarks" req)
  (POST "/bookmarks/search" req)
  (GET "/users/:id" [id]
       (api/get-user {:id id}))
  
  (route/not-found "Not Found"))

(defroutes unprotected-routes
  (POST "/users" req
        (api/add-user (:body req)))
  (GET "/token" req
       (api/get-token (-> (:headers req)
                          (get "authorization")
                          (clojure.string/split #" ")
                          (last)))))

(defn authenticated?
  [userid token]
  (api/authenticated? userid token))

(def app
  (routes
   (-> (wrap-defaults unprotected-routes api-defaults)
        (wrap-json-body {:keywords? true})
        (wrap-json-response))
    (-> (wrap-defaults app-routes api-defaults)
        (wrap-json-body {:keywords? true})
        (wrap-json-response)
        (wrap-basic-authentication authenticated?))))


