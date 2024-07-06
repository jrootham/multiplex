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
	(:require [ring-debug-logging.core :as debug])
	(:require [signup.common :as common])
	(:require [signup.stuff :as stuff])
	(:require [signup.form :as form])
)

(defn mail-config [from to subject body]
	{
		:to to
		:from from
		:subject subject
		:body [{:type "text/html" :content body}]
	}
)

(defn make-subject [index]
	(str "Test " index)
)

(defn make-response [result]
	(response/response (str (get result :code) " " (get result :error) " " (get result :message)))
)

(defn make-body [index]
	(page/html5 (str "This is test number " index))
)

(defn send-mail [address index]
	(let 
		[
			from (:user stuff/mailer)
			args (mail-config from address (make-subject index) (make-body index))
			result (postal/send-message stuff/mailer args)
		]
		(make-response result)
	)
)

(defn respond [bedrooms-str bathrooms-str spots-str size-str]
	(let
		[
			body [:div {:class "outer"} [:div {class "display"} "Check your email"]]
		]
		(str (hiccup/html body))
	)
)

(defn rent-string [bedrooms-str bathrooms-str spots-str size-str]
	(try
		(let 
			[
				bedrooms (Integer/parseInt bedrooms-str)
				bathrooms (Integer/parseInt bathrooms-str)
				spots (Integer/parseInt spots-str)
				size (Integer/parseInt size-str)
			]

			(form/rent-string bedrooms bathrooms spots size)
		)
		(catch NumberFormatException exception 
			(let 
				[
					message 
						(str 
							"An argument is not an Integer: bedrooms " bedrooms-str " ; bathrooms " ; bathrooms-str 
							" ; spots " spots-str " ; size " size-str)
					FOO (println message)
				]
				message
			)
		)
	)
)

(defn location-update [x y state]
	nil
)

(compojure/defroutes signup
	(compojure/GET "/signup.html" [key] (form/page key))
	(compojure/POST "/reload" [bedrooms bathrooms spots size] (respond bedrooms bathrooms spots size))
	(compojure/POST "/signup" [bedrooms bathrooms spots size] (respond bedrooms bathrooms spots size))
	(compojure/POST "/update" [bedrooms bathrooms spots size] (rent-string bedrooms bathrooms spots size))
	(compojure/POST "/location-update" [x y state] (location-update x y state))
	(compojure-route/not-found (list "Page not found"))
)

(defn handler []
	(-> signup
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

