(ns signup.action
	(:gen-class)
	(:require [next.jdbc :as jdbc])
	(:require [cheshire.core :as json])
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.element :as element])
	(:require [hiccup.form :as form])
	(:require [hiccup.util :as util])
	(:require [signup.common :as common])
	(:require [signup.forms :as forms])
	(:require [signup.mail :as mail])
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
				(forms/fragment (forms/message-body (get (Throwable->map exception) :cause)))
			)
		)
	)
)

(defn do-signup [db-name session]
	(forms/fragment 
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
		:body (forms/fragment (forms/prompt-contents session common/SIGNUP_CHOICE_TARGET))
	}
)

(defn duplicate []
	(forms/fragment "Email address already exists")
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
		(forms/fragment (forms/display-contents session))
		(forms/fragment "Update failed")
	)
)

(defn edit [db-name session]
	(common/execute-if-verified db-name session do-edit)
)

; (defn set-address [db-name session]
; 	(forms/fragment (forms/address-form ))
; )

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
					{
						:session new-session
						:body (forms/fragment (forms/display-contents new-session))
					}
					"Database update failed"
				)
			)
		)
	)
)

(defn do-mark-deleted [db-name session]
	(let
		[
			address (get session :address)
			sql "UPDATE applicant SET active=FALSE,verified=FALSE WHERE address = ?;"
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
	(common/execute-if-verified db-name session do-mark-deleted)
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
									{
										:session (assoc session :verified true)
										:body (forms/fragment (fill session))
									}
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
			(forms/fragment (get (Throwable->map exception) :cause))
		)
	)	
)

(defn copy-applicant [db-name session new-key address]
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
						columns "(created_at,magic_key,verified_at,name,address,bedrooms,bathrooms,parking,size,locations)"
						places "(?,?,?,?,?,?,?,?,?,CAST(? AS JSON))"
						sql (str "INSERT INTO applicant " columns " VALUES " places ";")
						statement [
												sql
												created-at
												new-key
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

(defn edit-address-verify [db-name session address]
	(let
		[
			old-address (get session :address)
			new-key (common/make-key)
			parameters {:key new-key :address address :old-address old-address}
			url (util/url common/CONFIRM_EDIT_ADDRESS_URL parameters)
		]
		(try
			(if (copy-applicant db-name session new-key address)
				(mail/send-mail url address)
				"Database error, no update made"
			)
			(catch Exception exception
				(println exception)
				(forms/fragment (forms/message-body (get (Throwable->map exception) :cause)))
			)
		)
	)
)

(defn confirm-address [db-name address old-address]
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
					{
						:session session
						:body (forms/base-page [:div form-def])
					}
				)
				(forms/base-page [:div "Loading session failed"])
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
				(if (common/set-inactive connection old-address)
					{
						:session new-session
						:body (forms/fragment(forms/display-contents session))
					}
					(forms/fragment "Database error")
				)
				(forms/fragment "Database error")
			)
		)
	)
)

(defn signon-mail [target db-name address]
	(with-open [connection (common/make-connection db-name)]
		(let 
			[
				read-sql "SELECT id FROM applicant WHERE address=? AND active;"
				read-statement [read-sql address]
				read-result (jdbc/execute-one! connection read-statement)
			]
			(if (seq read-result)
				(let 
					[
						new-key (common/make-key)
						url (util/url target {:key new-key :address address})
						write-sql "UPDATE applicant SET magic_key=? WHERE address=?;"
						write-statement [write-sql new-key address]
						write-result (jdbc/execute-one! connection write-statement)
					]
					(if (= 1 (get write-result :next.jdbc/update-count)) 
						(mail/send-mail url address)
						(forms/fragment "Database error")
					)
				)
				(forms/fragment (str address " not found"))
			)
		)
	)
)

(defn signon-action [db-name session call]
	(forms/fragment (call db-name session))
)
