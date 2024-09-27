(ns signup.mail
	(:gen-class)
	(:require [clojure.string :as string])
	(:require [next.jdbc :as jdbc])
	(:require [postal.core :as postal])
	(:require [hiccup.page :as page])
	(:require [hiccup.element :as element])
	(:require [hiccup.util :as util])
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

(defn make-url [referer target args]
	(let
		[
			pieces (string/split referer #"/")
			short (take (- (count pieces) 1) pieces)
			long (concat short (list target))
		]
		(util/url (string/join "/" long) args)
	)
)

(defn create-and-send-confirmation-email [referer target db-name address args]
	(with-open [connection (common/make-connection db-name)]
		(let
			[
				new-key (common/make-key)
				key-args (assoc args :key new-key)
				complete-args (assoc key-args :address address)
				url (make-url referer target complete-args)

				write-sql "UPDATE applicant SET magic_key=? WHERE address=?;"
				write-statement [write-sql new-key address]
				write-result (jdbc/execute-one! connection write-statement)
			] 
			(if (= 1 (get write-result :next.jdbc/update-count)) 
				(common/make-result (send-mail url address))
				(common/make-error "Database error")
			)
		)
	)
)
