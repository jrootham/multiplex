(ns signup.verify
	(:gen-class)
	(:require [next.jdbc :as jdbc])
	(:require [cheshire.core :as json])
;	(:require [hiccup2.core :as hiccup])
;	(:require [hiccup.page :as page])
;	(:require [hiccup.element :as element])
	(:require [hiccup.util :as util])
	(:require [signup.common :as common])
	(:require [signup.forms :as forms])
	(:require [signup.mail :as mail])
)

(defn confirm-button [target key address]
	[:div 
		[:button 
			{
				:hx-post (util/url target {:key key :address address})
				:hx-target "#contents"
			} 
			"Confirm email"
		]
	]
)

(defn needs-confirm [connection key address]
	(let 
		[
			sql "SELECT magic_key FROM applicant WHERE address=?;" 
			statement [sql address]
		]
		(let 
			[
				result (jdbc/execute-one! connection statement)
			]
			(if (seq result)
				(let [magic-key (get result :applicant/magic_key)]
					(if (nil? magic-key)
						common/CONFIRM_NO
						(if (= key magic-key)
							common/CONFIRM_YES
							(do 
								(println "Key mismatch for " address " key in email " key " in database " magic-key)
								common/CONFIRM_BAD
							)
						)
					)
				)
				(do 
					(println "Address not found for " address)
					common/CONFIRM_BAD
				)
			)
		)
	)
)

; (assoc session :verified true)

(defn verify [target db-name key address]
	(with-open [connection (common/make-connection db-name)]
		(let [result (needs-confirm connection key address)]
			(cond 
				(= result common/CONFIRM_YES) 
					(let [session common/load-session connection address]
						(forms/base-page (confirm-button target key address) session)
					)
				(= result common/CONFIRM_NO) (forms/error-page [:div "Email already confirmed"])
				(= result common/CONFIRM_BAD) (forms/error-page [:div "Something is wrong.  Contact system operator."])
			)
		)
	)
)

