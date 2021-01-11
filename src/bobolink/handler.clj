(ns bobolink.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [bobolink.api :as api]))

(defn get-in-req
  [request key]
  (get-in request [:body key]))

(defroutes app-routes
  (PUT "/bookmarks" req
       (response (api/add-bookmarks {} (get-in-req req :urls))))  
  (DELETE "/bookmarks" req)
  (GET "/bookmarks/search" req)
  (POST "/users" req
        (api/add-user (get-in-req req :user)))
  (GET "/users" req)
  (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)))

