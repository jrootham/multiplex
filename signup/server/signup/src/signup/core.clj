(ns signup.core
	(:gen-class)
	(:require [clojure.string :as string])
	(:require [ring.adapter.jetty :as jetty])
	(:require [ring.util.response :as response])
	(:require [ring.middleware.session :as session])
	(:require [ring.middleware.resource :as resource])
	(:require [ring.middleware.params :as params])
	(:require [compojure.core :as compojure])
	(:require [compojure.route :as compojure-route])
	(:require [next.jdbc :as jdbc])
	(:require [cheshire.core :as json])
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

(defn make-email-body [new-key]
	(page/html5 
		form/head
		[:body 
			[:div 
				"Click on the link to "
				(element/link-to (util/url common/VERIFY_URL {:key new-key}) "verify") 
				" your email"
			]
		]
	)
)

(defn send-mail [address new-key]
	(let 
		[
			from (:user stuff/mailer)
			args (mail-config from address common/MAIL_SUBJECT (make-email-body new-key))
		]
		(try
			(make-response (postal/send-message stuff/mailer args))
			(catch Exception exception
				(println exception)
				(get (Throwable->map exception) :cause)
			)
		)
	)
)

(defn new-applicant [connection session new-key]
	(let
		[
			columns "(magic_key,name,address,bedrooms,bathrooms,parking,size,locations)"
			places "(?,?,?,?,?,?,?,CAST(? AS JSON))"
			sql (str "INSERT INTO applicant " columns " VALUES " places ";")
			statement [
									sql
									new-key
									(get session :name)
									(get session :address)
									(get session :bedrooms)
									(get session :bathrooms)
									(get session :parking)
									(get session :size)
									(json/generate-string (get session :locations))
								]
		]
		(let [result (jdbc/execute-one! connection statement)]
			(= 1 (get result :next.jdbc/update-count)) 
		)
	)
)

(defn delete-applicant [connection id]
	(let
		[
			sql "DELETE FROM applicant WHERE id=?;"
			statement [sql id]
		]
		(jdbc/execute-one! connection statement)
	)
)

(defn create-applicant [db-name session new-key]
	(let 
		[
			address (get session :address)
			sql "SELECT id FROM applicant WHERE address=?;" 
			statement [sql address]
		]
		(with-open [connection (common/make-connection db-name)]
			(let 
				[
					result (jdbc/execute-one! connection statement)
				]
				(if (seq result)
					(delete-applicant connection (get result :applicant/id))
				)
				(new-applicant connection session new-key)
			)
		)
	)
)

(defn reload [db-name session]
	nil
)

(defn change-permanent-state [db-name session]
	(let
		[
			new-key (common/make-key)
			address (get session :address)
		]
		(try
			(if (create-applicant db-name session new-key)
				(send-mail address new-key)
				"Database error, no update made"
			)
			(catch Exception exception
				(println exception)
				(page/html5 form/head (form/error-body (get (Throwable->map exception) :cause)))
			)
		)
	)
)

(defn do-signup [db-name session]
	(let
		[
			body [:div {:class "outer"} [:div {class "display"} (change-permanent-state db-name session)]]
		]
		(str (hiccup/html body))
	)
)

(defn identity-session [session name address]
	(-> session
		(assoc :address address)
		(assoc :name name)
	)
)

(defn identity-result [session]
	{
		:session session
		:body (str (hiccup/html (form/prompt-contents session)))
	}
)

(defn no-applicant [db-name address]
	(let
		[
			sql (str "SELECT COUNT(*) FROM applicant WHERE address=? AND verified;")
			statement [sql address]
		]
		(with-open [connection (common/make-connection db-name)]
			(let [result (jdbc/execute-one! connection statement)]
				(= 0 (get result :count)) 
			)
		)
	)
)

(defn existing-applicant [db-name address session]
	(let
		[
			sql (str "SELECT active FROM applicant WHERE address=? AND verified;")
			statement [sql address]
		]
		(with-open [connection (common/make-connection db-name)]
			(let [result (jdbc/execute-one! connection statement)]
				(let [active (get result :applicant/active)]
					(if active
						(identity-result session)
						(identity-result session)
					)
				)
			)
		)
	)
)

(defn set-identity [db-name session name address]
	(let [new-session (identity-session session name address)]
		(if (no-applicant db-name address)
			(identity-result new-session)
			(existing-applicant db-name address new-session)
		)
	)
)

(defn update-data [session bedrooms-str bathrooms-str parking-str size-str]
	(try
		(let 
			[
				bedrooms (Integer/parseInt bedrooms-str)
				bathrooms (Integer/parseInt bathrooms-str)
				parking (Integer/parseInt parking-str)
				size (Integer/parseInt size-str)
			]=

			{
				:session (common/update-session session bedrooms bathrooms parking size)
				:body (form/rent-string bedrooms bathrooms parking size)
			}			
		)
		(catch NumberFormatException exception 
			(let 
				[
					message 
						(str 
							"An argument is not an Integer: bedrooms " bedrooms-str " ; bathrooms " ; bathrooms-str 
							" ; parking " parking-str " ; size " size-str
						)
				]
				message
			)
		)
	)
)

(compojure/defroutes signup
	(compojure/GET "/signup.html" [] (form/new-page))
	(compojure/GET "/update.html" [db-name key] (form/page db-name key))
	(compojure/GET "/verify.html" [db-name key] (form/verify db-name key))
	(compojure/POST "/reload" [db-name session] (reload db-name session))
	(compojure/POST "/signup" [db-name session] (do-signup db-name session))
	(compojure/POST "/identity" 
		[db-name session name address] 
		(set-identity db-name session name address))
	(compojure/POST "/update" 
		[session bedrooms bathrooms parking size] 
		(update-data session bedrooms bathrooms parking size)
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

