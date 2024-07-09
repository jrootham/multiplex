(ns signup.core
	(:gen-class)
	(:require [clojure.string :as string])
	(:require [ring.adapter.jetty :as jetty])
	(:require [ring.util.response :as response])
	(:require [ring.middleware.session :as session])
	(:require [ring.middleware.resource :as resource])
;	(:require [ring.middleware.content-type :as content-type])
;	(:require [ring.middleware.not-modifed :as not-modifed])
	(:require [ring.middleware.params :as params])
	(:require [compojure.core :as compojure])
	(:require [compojure.route :as compojure-route])
	(:require [postal.core :as postal])
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.element :as element])
	(:require [hiccup.util :as util])
	(:require [crypto.random :as random])
	(:require [signup.common :as common])
	(:require [signup.stuff :as stuff])
	(:require [signup.form :as form])
	(:require [signup.location :as location])
	(:require [ring-debug-logging.core :as debug])
)

(def MAIL_SUBJECT "Co-op verify email")
(def KEY_SIZE 16)
(def VERIFY_URL "https://jrootham.ca/multiplex/server/verify.html")

(defn mail-config [from to subject body]
	{
		:to to
		:from from
		:subject subject
		:body [{:type "text/html" :content body}]
	}
)

(defn make-response [result]
	"Go check your email"
)

(defn make-email-body []
	(page/html5 
		form/head
		[:body 
			[:div 
				"Click on the link to "
				(element/link-to (util/url VERIFY_URL {:key (random/hex KEY_SIZE)}) "verify")]
				" your email"
			]
	)
)

(defn send-mail [address]
	(let 
		[
			foo (println address)
			from (:user stuff/mailer)
			args (mail-config from address MAIL_SUBJECT (make-email-body))
		]
		(try
			(make-response (postal/send-message stuff/mailer args))
			(catch Exception exception
				(println exception)
			)
		)
	)
)

(defn reload [session]
	nil
)

(defn do-signup [session]
	(let
		[
			address (get session :address)
			body [:div {:class "outer"} [:div {class "display"} (send-mail address)]]
		]
		(str (hiccup/html body))
	)
)

(defn update-data [session address bedrooms-str bathrooms-str spots-str size-str]
	(try
		(let 
			[
				bedrooms (Integer/parseInt bedrooms-str)
				bathrooms (Integer/parseInt bathrooms-str)
				spots (Integer/parseInt spots-str)
				size (Integer/parseInt size-str)
			]

			{
				:session (common/update-session session address bedrooms bathrooms spots size)
				:body (form/rent-string bedrooms bathrooms spots size)
			}			
		)
		(catch NumberFormatException exception 
			(let 
				[
					message 
						(str 
							"An argument is not an Integer: bedrooms " bedrooms-str " ; bathrooms " ; bathrooms-str 
							" ; spots " spots-str " ; size " size-str)
				]
				message
			)
		)
	)
)

(compojure/defroutes signup
	(compojure/GET "/signup.html" [] (form/new-page))
	(compojure/GET "/update.html" [key] (form/page key))
	(compojure/GET "/verify.html" [key] (form/verify key))
	(compojure/POST "/reload" [session] (reload session))
	(compojure/POST "/signup" [session] (do-signup session))
	(compojure/POST "/update" 
		[session address bedrooms bathrooms spots size] 
		(update-data session address bedrooms bathrooms spots size)
	)
	(compojure/POST "/location" [session x y] (location/update-location session x y))
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

(defn handler []
	(-> signup
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
  	(if (== 1 (count args))
		(let 
			[
				port-string (nth args 0)
			]
			(try
				(let 
					[
						port (Integer/parseInt port-string)
					]
					(jetty/run-jetty (handler) {:port port})
				)
				(catch NumberFormatException exception 
					(println (str port-string " is not an int"))
				)
			)
		)  	
		(println "lein run signup port")
	)
)

