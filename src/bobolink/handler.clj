(ns bobolink.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]))

(defroutes app-routes
  (PUT "/bookmarks" request
       (response (:body request)))
  (DELETE "/bookmarks" [])
  (GET "/bookmarks/search" [])
  (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)))
