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
	(:require [signup.verify :as verify])
	(:require [signup.forms :as forms])
	(:require [signup.location :as location])
	(:require [ring-debug-logging.core :as debug])
)

(compojure/defroutes signup

; create application by getting name and address

	(compojure/GET "/signup.html" [] (forms/new-page))
	(compojure/POST "/identity" 
		[db-name session name address] 
		(action/set-identity db-name session name address)
	)

; choices loop

	(compojure/POST "/update" 
		[session bedrooms bathrooms parking size] 
		(action/update-data session bedrooms bathrooms parking size)
	)
	(compojure/POST "/location" [session x y] (location/update-location session x y))

; verify address and display current state

	(compojure/POST "/signup" [db-name session] (action/do-signup db-name session))
	(compojure/GET "/verify.html" 
		[db-name key address] 
		(verify/verify common/SIGNUP_CONFIRM_TARGET db-name key address)
	)

	(compojure/POST "/confirm" 
		[db-name key address] 
		(action/confirm db-name key address forms/display-contents)
	)

; edit choices

	(compojure/POST "/edit-prompt" [db-name session] (forms/edit-prompt db-name session))
	(compojure/POST "/edit" [db-name session] (action/edit db-name session))

; edit address

	(compojure/POST "/address-prompt" [db-name session] (forms/address-prompt db-name session))
	(compojure/POST "/edit-address-verify" 
		[db-name session address] 
		(action/edit-address-verify db-name session address)
	)
	(compojure/GET "/confirm-address.html" 
		[db-name address old-address] 
		(action/confirm-address db-name address old-address)
	)
	(compojure/POST "/address" 
		[db-name session address old-address] 
		(action/set-address db-name session address old-address)
	)

; edit name

	(compojure/POST "/name-prompt" [db-name session] (forms/name-prompt db-name session))
	(compojure/POST "/name" [db-name session name] (action/set-name db-name session name))

; delete

	(compojure/POST "/delete" [db-name session] (action/mark-deleted db-name session))

;*********************************************************
; Do signon from exterior links then perform action
;*********************************************************

; Display
	(compojure/GET "/display.html" [] (forms/signon "display-mail"))
	(compojure/POST "/display-mail" 
		[db-name address] 
		(action/signon-mail common/DISPLAY_CONFIRM_HTML db-name address)
	)
	(compojure/GET "/display-confirm.html" 
		[db-name key address] 
		(forms/signon-confirm db-name key address "display-action")
	)
	(compojure/POST "/display-action"
		[db-name session] 
		(action/signon-action db-name session forms/do-display)
	)

; Edit address

	(compojure/GET "/edit-address.html" [] (forms/signon "edit-address-mail"))
	(compojure/POST "/edit-address-mail" 
		[db-name address] 
		(action/signon-mail common/EDIT_ADDRESS_CONFIRM_HTML db-name address)
	)
	(compojure/GET "/edit-address-confirm.html" 
		[db-name key address] 
		(forms/signon-confirm db-name key address "edit-address-action")
	)
	(compojure/POST "/edit-address-action"
		[db-name session] 
		(action/signon-action db-name session forms/call-address-prompt)
	)

; Edit name

	(compojure/GET "/edit-name.html" [] (forms/signon "edit-name-mail"))
	(compojure/POST "/edit-name-mail" 
		[db-name address] 
		(action/signon-mail common/EDIT_NAME_CONFIRM_HTML db-name address)
	)
	(compojure/GET "/edit-name-confirm.html" 
		[db-name key address] 
		(forms/signon-confirm db-name key address "edit-name-action")
	)
	(compojure/POST "/edit-name-action"
		[db-name session] 
		(action/signon-action db-name session forms/call-name-prompt)
	)

; Edit choices

	(compojure/GET "/edit-choices.html" [] (forms/signon "edit-choices-mail"))
	(compojure/POST "/edit-choices-mail" 
		[db-name address] 
		(action/signon-mail common/EDIT_CHOICES_CONFIRM_HTML db-name address)
	)
	(compojure/GET "/edit-choices-confirm.html" 
		[db-name key address] 
		(forms/signon-confirm db-name key address "edit-choices-action")
	)
	(compojure/POST "/edit-choices-action"
		[db-name session] 
		(action/signon-action db-name session forms/call-edit-prompt)
	)

; Delete

	(compojure/GET "/delete.html" [] (forms/signon "delete-mail"))
	(compojure/POST "/delete-mail" 
		[db-name address] 
		(action/signon-mail common/DELETE_CONFIRM_HTML db-name address)
	)
	(compojure/GET "/delete-confirm.html" 
		[db-name key address] 
		(forms/signon-confirm db-name key address "delete-action")
	)
	(compojure/POST "/delete-action"
		[db-name session] 
		(action/signon-action db-name session action/do-mark-deleted)
	)

	(compojure-route/not-found (forms/base-page "Page not found"))
)

(defn wrap-trace [handler]
	(fn [request]
		(let 
			[
				foo (println (get request :uri))
			]
			(handler request)
		)
	)
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
;		(wrap-trace)
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

