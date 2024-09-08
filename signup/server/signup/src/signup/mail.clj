(ns signup.mail
	(:gen-class)
	(:require [postal.core :as postal])
	(:require [hiccup.page :as page])
	(:require [hiccup.element :as element])
	(:require [signup.common :as common])
	(:require [signup.forms :as forms])
	(:require [signup.stuff :as stuff])
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

(defn make-email-body [url]
	(page/html5 
		forms/head
		[:body 
			[:div 
				"Click on the link to "
				(element/link-to url "verify") 
				" your email"
			]
		]
	)
)

(defn send-mail [url address]
	(let 
		[
			from (:user stuff/mailer)
			args (mail-config from address common/MAIL_SUBJECT (make-email-body url))
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

