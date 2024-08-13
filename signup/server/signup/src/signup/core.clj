(ns signup.core
	(:gen-class)
	(:require [ring.adapter.jetty :as jetty])
	(:require [ring.util.response :as response])
	(:require [ring.middleware.session :as session])
	(:require [ring.middleware.resource :as resource])
	(:require [ring.middleware.params :as params])
	(:require [compojure.core :as compojure])
	(:require [compojure.route :as compojure-route])
	(:require [signup.common :as common])
	(:require [signup.action :as action])
	(:require [signup.form :as form])
	(:require [signup.location :as location])
	(:require [ring-debug-logging.core :as debug])
)

(compojure/defroutes signup
	(compojure/GET "/signup.html" [] (form/new-page))
	(compojure/GET "/edit.html" [] (form/edit-page))
	(compojure/GET "/delete.html" [] (form/delete-page))
	(compojure/GET "/update.html" [db-name key] (form/page db-name key))
	(compojure/GET "/verify.html" 
		[db-name key address] 
		(action/verify common/SIGNUP_CONFIRM_TARGET db-name key address))
	(compojure/GET "/verifyidentity.html" 
		[db-name key address] 
		(action/verify common/EDIT_IDENTITY_PROMPT db-name key address))
	(compojure/GET "/verifychoices.html" 
		[db-name key address] 
		(action/verify common/EDIT_CHOICE_PROMPT db-name key address))
	(compojure/GET "/verifydelete.html" 
		[db-name key address] 
		(action/verify common/DELETE_TARGET db-name key address))
	(compojure/POST "/confirm" [db-name key address] (action/confirm db-name key address))
	(compojure/POST "/signup" [db-name session] (action/do-signup db-name session))
	(compojure/POST "/identity" 
		[db-name session name address] 
		(action/set-identity db-name session name address))
	(compojure/POST "/update" 
		[session bedrooms bathrooms parking size] 
		(action/update-data session bedrooms bathrooms parking size)
	)
	(compojure/POST "/location" [session x y] (location/update-location session x y))
	(compojure/POST "/delete" [db-name session] (action/mark-deleted db-name session))
	(compojure/POST "/identityprompt" [db-name session] (action/identity-prompt db-name session))
	(compojure/POST "/editprompt" [db-name session] (action/edit-prompt db-name session))
	(compojure/POST "/edit" [db-name session] (action/edit db-name session))
	(compojure/POST "/delete" [db-name session] (action/mark-deleted db-name session))
	(compojure-route/not-found (list "Page not found"))
)

(defn wrap-get-session [handler]
	(fn [request]
		(let 
		[
			params (get request :params)
			session (get request :session)
		]
			(handler (assoc request :params (assoc params :session session)))
		)
	)
)

(defn wrap-db-name [handler db-name]
	(fn [request]
		(let [params (get request :params)]
			(handler (assoc request :params (assoc params :db-name db-name)))
		)
	)
)

(defn handler [db-name]
	(-> signup
		(wrap-db-name db-name)
		(wrap-get-session)
		(session/wrap-session)
		(params/wrap-params)
    (resource/wrap-resource "public")
;		(debug/wrap-with-logger)
	)
)

(defn -main
  "signup server"
  [& args]
  	(if (== 2 (count args))
		(let 
			[
				port-string (nth args 0)
				db-name (nth args 1)
			]
			(try
				(let 
					[
						port (Integer/parseInt port-string)
					]
					(jetty/run-jetty (handler db-name) {:port port})
				)
				(catch NumberFormatException exception 
					(println (str port-string " is not an int"))
				)
			)
		)  	
		(println "lein run signup port database")
	)
)

