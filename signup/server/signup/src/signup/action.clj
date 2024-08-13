(ns signup.action
	(:gen-class)
	(:require [next.jdbc :as jdbc])
	(:require [cheshire.core :as json])
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.element :as element])
	(:require [hiccup.util :as util])
	(:require [signup.common :as common])
	(:require [signup.form :as form])
	(:require [signup.mail :as mail])
)

(defn new-applicant [connection session new-key]
(println "new-applicant" new-key)
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

(defn needs-confirm [db-name key address]
	(let 
		[
			sql "SELECT magic_key FROM applicant WHERE address=?;" 
			statement [sql address]
		]
		(with-open [connection (common/make-connection db-name)]
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
)

(defn verify [target db-name key address]
	(let [result (needs-confirm db-name key address)]
		(cond 
			(= result common/CONFIRM_YES) (form/base-page (confirm-button target key address))
			(= result common/CONFIRM_NO) (form/base-page [:div "Email already confirmed"])
			(= result common/CONFIRM_BAD) (form/base-page [:div "Something is wrong.  Contact system operator."])
		)
	)
)

(defn change-permanent-state [db-name session]
	(let
		[
			new-key (common/make-key)
			address (get session :address)
			url (util/url common/VERIFY_SIGNUP_URL {:key new-key :address address})
		]
		(try
			(if (create-applicant db-name session new-key)
				(mail/send-mail url address)
				"Database error, no update made"
			)
			(catch Exception exception
				(println exception)
				(form/fragment (form/message-body (get (Throwable->map exception) :cause)))
			)
		)
	)
)

(defn do-signup [db-name session]
	(form/fragment 
		[:div 
			{:class "outer"} 
			[:div 
				{class "display"} 
				(change-permanent-state db-name session)
			]
		]
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
		:body (form/fragment (form/prompt-contents session common/SIGNUP_CHOICE_TARGET))
	}
)

(defn duplicate []
	(form/fragment "Email address already exists")
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
						(duplicate)
						(identity-result session)
					)
				)
			)
		)
	)
)

(defn set-identity [db-name session name address]
(println "set-identity" name address)
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
			]
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

(defn execute-if-verified [db-name session action]
	(if (get session :verified)
		(action db-name session)
		"Session is not verified"
	)	
)

(defn do-edit-prompt [db-name session]
	{
		:session session
		:body (form/fragment (form/prompt-contents session common/EDIT_CHOICE_TARGET))
	}
)

(defn edit-db-update [db-name session]
	(with-open [connection (common/make-connection db-name)]
		(let
			[
				columns "bedrooms=?,bathrooms=?,parking=?,size=?,locations=CAST(? AS JSON)"
				where "WHERE address=?;"
				sql (str "UPDATE applicant SET " columns " " where)
				statement [
										sql
										(get session :bedrooms)
										(get session :bathrooms)
										(get session :parking)
										(get session :size)
										(json/generate-string (get session :locations))
										(get session :address)
									]
			]
			(let [result (jdbc/execute-one! connection statement)]
				(= 1 (get result :next.jdbc/update-count)) 
			)
		)
	)
)

(defn do-edit [db-name session]
	(if (edit-db-update db-name session)
		(form/fragment (form/display-contents session))
		(form/fragment "Update failed")
	)
)

(defn edit [db-name session]
	(execute-if-verified db-name session do-edit)
)

(defn do-identity-prompt [db-name session]
	(form/identity-form session)
)

(defn edit-prompt [db-name session]
	(execute-if-verified db-name session do-edit-prompt)
)

(defn identity-prompt [db-name session]
	(execute-if-verified db-name session do-identity-prompt)
)

(defn do-mark-deleted [db-name session]
	(let
		[
			address (get session :address)
			sql "UPDATE applicant SET active=FALSE WHERE address = ?;"
			statement [sql address]
		]
		(with-open [connection (common/make-connection db-name)]
			(let [result (jdbc/execute-one! connection statement)]
				(if (= 1 (get result :next.jdbc/update-count))
					(str "Applicant with " address " has been marked inactive.")
					"Database update failed"
				)
			)
		)
	)
)

(defn mark-deleted [db-name session]
	(execute-if-verified db-name session do-mark-deleted)
)

(defn confirm [db-name key address]
	(println "confirm" key address)
	(try
		(with-open [connection (common/make-connection db-name)]
			(if (common/key-checked connection address key)
				(let [verified (common/mark-verified connection address)]
					(if verified
						(let [result (common/load-session connection address)]
							(if (get result :success)
								(let 
									[
										session (get result :session)
									]
									{
										:session (assoc session :verified true)
										:body (form/fragment (form/display-contents session))
									}
								)
								(form/error-output "Load failed"	"Internal error")
							)
						)
						(form/error-output "Verify failed"	"Internal error")
					)
				)
				(form/error-output "Key check failed"	"Internal error")
			)
		)
		(catch Exception exception
			(println exception)
			(form/fragment (get (Throwable->map exception) :cause))
		)
	)	
)
