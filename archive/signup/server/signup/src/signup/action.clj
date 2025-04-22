(ns signup.action
	(:gen-class)
	(:require [next.jdbc :as jdbc])
	(:require [cheshire.core :as json])
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.element :as element])
	(:require [hiccup.form :as form])
	(:require [signup.common :as common])
	(:require [signup.forms :as forms])
	(:require [signup.mail :as mail])
)

(defn new-applicant [connection session]
	(let
		[
			columns "(name,address,bedrooms,bathrooms,parking,size,locations)"
			places "(?,?,?,?,?,?,CAST(? AS JSON))"
			sql (str "INSERT INTO applicant " columns " VALUES " places ";")
			statement [
									sql
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

(defn create-applicant [db-name session]
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
				(new-applicant connection session)
			)
		)
	)
)

(defn change-permanent-state [referer db-name session]
	(let [address (get session :address)]
		(try
			(if (create-applicant db-name session)
				(mail/create-and-send-confirmation-email referer common/VERIFY_SIGNUP_HTML db-name address {})
				(common/make-error "Database error, no update made")
			)
			(catch Exception exception
				(println exception)
				(common/make-error (forms/message-body (get (Throwable->map exception) :cause)))
			)
		)
	)
)

(defn send-fragment [body session]
	(forms/fragment 
		[:div 
			{:class "outer"} 
			[:div 
				{class "display"} 
				body
			]
		]
		session
	)
)

(defn do-signup [referer db-name session]
	(let 
		[
			return (change-permanent-state referer db-name session)
			success (get return :success)
			result (get return :result)
		]
		(if success
			(send-fragment result nil)
			(forms/error-fragment result)
		)
	)
)

(defn identity-session [session name address]
	(-> session
		(assoc :address address)
		(assoc :name name)
	)
)

(defn identity-result [session]
	(forms/fragment (forms/prompt-contents session common/SIGNUP_CHOICE_TARGET) session)
)

(defn duplicate []
	(forms/error-fragment "Email address already exists")
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
			sql (str "SELECT COUNT(*) FROM applicant WHERE address=? AND verified;")
			statement [sql address]
		]
		(with-open [connection (common/make-connection db-name)]
			(let [result (jdbc/execute-one! connection statement)]
				(if (= (get result :count) 0)
					(identity-result session)
					(duplicate)
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
			]
			{
				:session (common/update-session session bedrooms bathrooms parking size)
				:body (forms/rent-string bedrooms bathrooms parking size)
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
		(forms/fragment (forms/display-contents session) session)
		(forms/error-fragment "Update failed")
	)
)

(defn edit [db-name session]
	(common/execute-if-verified db-name session do-edit)
)

(defn set-name [db-name session name]
	(let
		[
			address (get session :address)
			sql "UPDATE applicant SET name=? WHERE address = ?;"
			statement [sql name address]
			new-session (assoc session :name name)
		]
		(with-open [connection (common/make-connection db-name)]
			(let [result (jdbc/execute-one! connection statement)]
				(if (= 1 (get result :next.jdbc/update-count))
					(forms/fragment (forms/display-contents new-session) new-session)
					(forms/error-fragment "Database update failed")
				)
			)
		)
	)
)

(defn delete [connection address]
	(let
		[
			sql "DELETE FROM applicant WHERE address = ?;"
			statement [sql address]
		]
		(let [result (jdbc/execute-one! connection statement)]
			(= 1 (get result :next.jdbc/update-count))
		)
	)
)

(defn do-delete [db-name session]
	(with-open [connection (common/make-connection db-name)]
		(let [address (get session :address)]
			(if (delete connection address)
				(str "Applicant with " address " has been deleted.")
				"Database update failed"
			)
		)
	)
)
 
(defn delete-record [db-name session]
	(common/execute-if-verified db-name session do-delete)
)

(defn confirm [db-name key address fill]
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
									(forms/fragment (fill session) session)
								)
								(forms/error-output "Load failed" "Internal error")
							)
						)
						(forms/error-output "Verify failed" "Internal error")
					)
				)
				(forms/error-output "Key check failed"	"Internal error")
			)
		)
		(catch Exception exception
			(println exception)
			(forms/error-fragment (get (Throwable->map exception) :cause))
		)
	)	
)

(defn copy-applicant [db-name session address]
	(with-open [connection (common/make-connection db-name)]
		(let
			[
				old-address (get session :address)
				read-columns "(created_at,verified_at)"
				read-sql (str "SELECT " read-columns " FROM applicant WHERE address=?;")
				read-statement [read-sql old-address]
				read-result (jdbc/execute-one! connection read-statement)
			]
			(if (seq read-result)
				(let
					[
						created-at (get read-result :applicant/created_at)
						verified-at (get read-result :applicant/verified_at)
						columns "(created_at,verified_at,name,address,bedrooms,bathrooms,parking,size,locations)"
						places "(?,?,?,?,?,?,?,?,CAST(? AS JSON))"
						sql (str "INSERT INTO applicant " columns " VALUES " places ";")
						statement [
												sql
												created-at
												verified-at
												(get session :name)
												address
												(get session :bedrooms)
												(get session :bathrooms)
												(get session :parking)
												(get session :size)
												(json/generate-string (get session :locations))
											]
						result (jdbc/execute-one! connection statement)
					]
					(= 1 (get result :next.jdbc/update-count)) 
				)
				false
			)
		)
	)
)

(defn edit-address-verify [referer db-name session address]
	(let
		[
			old-address (get session :address)
			parameters {:old-address old-address}
		]
		(try
			(if (copy-applicant db-name session address)
				(let 
				[
					return (mail/create-and-send-confirmation-email 
									referer common/CONFIRM_EDIT_ADDRESS_HTML db-name address parameters)
					success (get return :success)
					result (get return :result)
				]
					(if success
						(forms/fragment result nil)
						(forms/error-fragment result)
					)
				)
				(forms/error-fragment "Database error, no update made")
			)
			(catch Exception exception
				(println exception)
				(forms/error-fragment (forms/message-body (get (Throwable->map exception) :cause)))
			)
		)
	)
)

(defn confirm-address [db-name key address old-address]
	(with-open [connection (common/make-connection db-name)]
		(let [load-result (common/load-session connection address)]
			(if (get load-result :success)
				(let
					[
						session (get load-result :session)
						address-field (form/hidden-field "address" address)
						old-address-field (form/hidden-field "old-address" old-address)
						submit (forms/confirm-email common/EDIT_ADDRESS_TARGET)
						form-content [:div address-field old-address-field submit]
						form-def (form/form-to [:post common/EDIT_ADDRESS_TARGET] form-content)
					]
						(forms/base-page [:div form-def] session)
				)
				(forms/error-page [:div "Loading session failed"])
			)
		)
	)
)

(defn address-session [session address]
	(-> session
		(assoc :address address)
		(assoc :verified true)
	)
)

(defn set-address [db-name session address old-address]
	(with-open [connection (common/make-connection db-name)]
		(let [new-session (address-session session address)]
			(if (common/set-verified connection address)
				(if (delete connection old-address)
					(forms/fragment(forms/display-contents new-session) new-session)
					(forms/error-fragment "Database error, failed to delete old address")
				)
				(forms/error-fragment "Database error, failed to set verified")
			)
		)
	)
)

(defn signon-mail [referer target db-name address]
	(with-open [connection (common/make-connection db-name)]
		(let 
			[
				read-sql "SELECT id FROM applicant WHERE address=?;"
				read-statement [read-sql address]
				read-result (jdbc/execute-one! connection read-statement)
			]
			(if (seq read-result)
				(let
					[
						return (mail/create-and-send-confirmation-email referer target db-name address {})
						success (get return :success)
						result (get return :result)
					]
					(if success
						(forms/fragment result nil)
						(forms/error-fragment result)
					)
				)
				(forms/error-fragment (str address " not found"))
			)
		)
	)
)

(defn signon-action [db-name session call]
	(forms/fragment (call db-name session) session)
)
