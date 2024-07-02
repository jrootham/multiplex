(ns signup.core
	(:gen-class)
	(:require [clojure.string :as string])
	(:require [ring.adapter.jetty :as jetty])
	(:require [ring.util.response :as response])
	(:require [ring.middleware.session :as session])
	(:require [ring.middleware.params :as params])
	(:require [compojure.core :as compojure])
	(:require [compojure.route :as compojure-route])
	(:require [postal.core :as postal])
	(:require [signup.stuff :as stuff])
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [ring-debug-logging.core :as debug])
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

(defn respond [bedrooms bathrooms lots size]
	(let
		[
			foo (println bedrooms bathrooms lots size)
		]

		"Check your email"
	)
)

(compojure/defroutes signup
	(compojure/POST "/signup" [bedrooms bathrooms lots size] (respond bedrooms bathrooms lots size))
	(compojure-route/not-found (list "Page not found"))
)

(defn handler []
	(-> signup
		(session/wrap-session)
		(params/wrap-params)
		(debug/wrap-with-logger)
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

