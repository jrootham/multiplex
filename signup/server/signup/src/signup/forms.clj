(ns signup.forms
	(:gen-class)
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.form :as form])
	(:require [signup.common :as common])
	(:require [signup.location :as location])
)

(def BEDROOM_VALUES (range 1 6))
(def BATHROOM_VALUES (range 1 4))
(def PARKING_VALUES (range 0 4))
(def SIZE_VALUES (range 50 210 10))

(def SIMPLE_LABELS (vector "No " "One " "Two " "Three " "Four " "Five " "Six " "Seven " "Eight " "Nine " "Ten "))

(defn simple-label [base-name value]
	(str (nth SIMPLE_LABELS value) base-name)
)

(defn size-label [base-name value]
	(format "%d sq m (%d sq ft)" value (* value 10))
)

(defn htmx []
	[:script
		{
			:src "https://unpkg.com/htmx.org@1.9.12" 
			:integrity "sha384-ujb1lZYygJmzgSwoxRggbCHcjc0rB2XoQrxeTUQyRjrOnlCoYta87iKBWq3EsdM2" 
			:crossorigin "anonymous"
		}
	]
)

(def head
	[:head
		[:meta {:charset "utf-8"}]
		[:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
		(page/include-css "style.css")
		[:title "Multiplex Signup"]
		(htmx)
	]
)

(defn fragment [html session]
	{
		:session session
		:body (str (hiccup/html html))
	}	
)

(defn error-fragment [html]
	(fragment html nil)
)

(defn rent-string [bedrooms bathrooms parking size]
	(let
		[
			rent
				(+ 
					(* common/BEDROOM-COST bedrooms) 
					(* common/BATHROOM-COST bathrooms) 
					(* common/SIZE-COST size) 
					(* common/PARKING-COST parking) 
					(* common/KITCHEN-COST 1)
				)
			loan (* common/LOAN-MONTHS rent)
		]
		(str "Expected rent: " (format "$%,d" (int rent)) " Expected Member Loan: " (format "$%,d" (int loan)))
	)
)

(defn make-radio-map [name-base label-fn current-value]
	(fn [value] 
		(let 
			[
				name (str name-base "-" value)
			] 
			[:div
				(form/radio-button name-base (= current-value value) value)
				(form/label name (label-fn name-base value))
			]
		)
	)
)

(defn pick-bedrooms [bedrooms]
	[:div {:class "choose"}
		[:fieldset 
			[:legend "Select number of bedrooms"]
			(map (make-radio-map "bedrooms" simple-label bedrooms) BEDROOM_VALUES)
		]
	]
)

(defn pick-bathrooms [bathrooms]
	[:div {:class "choose"}
		[:fieldset 
			[:legend "Select number of bathrooms"]
			(map (make-radio-map "bathrooms" simple-label bathrooms) BATHROOM_VALUES)
		]
	]
)

(defn pick-parking [parking]
	[:div {:class "choose"}
		[:fieldset 
			[:legend "Select parking spaces"]
			(map (make-radio-map "parking" simple-label parking) PARKING_VALUES)
		]
	]
)

(defn pick-size [size]
	[:div {:class "choose"}
		[:fieldset 
			[:legend "Select size of unit"]
			(map (make-radio-map "size" size-label size) SIZE_VALUES)
		]
	]
)

(defn legend [text class]
	[:div {:class "legendbox"}
		[:div {:class "legend"} text]
		[:div {:class "outersample"} [:div {:class class}]]
		[:div {:class "buffer"}]
	]
)

(defn location-docs []
	[:div {:class "docdiv"}
		[:div {:class "doctext"}
			(str
				"The map below is for you to indicate your preferences about where you want to live in the city. "
				"The city is divided into a 20x8 grid of tiles. "
				"The background of each tile is coloured according to how much you indicated "
				"that you want to live there."
			)
		]
		[:div 
			(legend "0: I don't want to live there " "sample")
			(legend "1: I would prefer not to live there  " "sample mode1")
			(legend "2: I would be fine with living there " "sample mode2")
			(legend "3: I really want to live there " "sample mode3")
		]

		[:div {:class "doctext"}
			(str
				"Each tile starts at level 0. "
				"Clicking on a tile adds one to the level, unless it is at level 3, in which case it goes back to level 0."
			)
		]
	]

)

(defn confirm-email [target]
	(form/submit-button 
		{
			:hx-post target
			:hx-target "#contents"
		} 
		"Confirm email"
	)
)

(defn submit [target]
	(form/submit-button 
		{
			:hx-post target
			:hx-target "#contents"
		} 
		"Submit"
	)
)

(defn identity-form [session]
	(let 
		[
			{
				name :name
				address :address
			} session
		]
		[:div
			[:form 
				{
					:hx-post "/multiplex/server/identity"
					:hx-target "#contents"
				}
				[:div {:class "identity-layout"}
					[:div "Name"]
					[:div (form/text-field "name") name]
					[:div "Email address"]
					[:div (form/email-field {:required "true"} "address" address)]
				]
				[:button "Set"]
			]
		]
	)
)

(defn prompt-contents [session target]
	(let 
		[
			{
				name :name
				address :address
				bedrooms :bedrooms
				bathrooms :bathrooms 
				parking :parking
				size :size
				locations :locations
			} session
		]
		[:div
			[:div "For " name " from " address]
			[:div {:id "costs"} (rent-string bedrooms bathrooms parking size)]

			[:form {:hx-post "/multiplex/server/update" :hx-trigger "change" :hx-target "#costs"}
				(pick-bedrooms bedrooms)
				(pick-bathrooms bathrooms)
				(pick-parking parking)
				(pick-size size)
			]

			; (location-docs)
			; (location/location-map session location/tile)

			(submit target)
		]
	)
)

(defn address-form [target address]
	[:div
		[:div "Email address"]
		[:form
			{
				:hx-post target
				:hx-target "#contents"
			}
			[:div 
				(form/email-field {:required "true"} "address" address)
				[:button "Submit"]
			]
		]
	]
)

(defn name-form [target name]
	[:div
		[:div "Name"]
		[:form
			{
				:hx-post target
				:hx-target "#contents"
			}
			[:div 
				(form/text-field {:required "true"} "name" name)
				[:button "Submit"]
			]
		]
	]
)

(defn action-button [action text]
	[:button 
		{
			:hx-post action 
			:hx-target "#contents"
		} 
		text
	]
)

(defn display-contents [session]
	(let 
		[
			{
				name :name
				address :address
				bedrooms :bedrooms
				bathrooms :bathrooms 
				parking :parking
				size :size
				locations :locations
			} session
		]
		[:div
			[:div "For " name " at " address]
			[:div {:id "costs"} (rent-string bedrooms bathrooms parking size)]
			[:div 
				(format 
					"Bedrooms %d bathrooms %d parking %d size %d sq m (%,d sq ft)"
					bedrooms bathrooms parking size (* 10 size)
				)
			]
;			[:div (location/location-map session location/display-tile)]
			[:div 
				(action-button common/EDIT_ADDRESS_PROMPT "Edit email")
				(action-button common/EDIT_NAME_PROMPT "Edit name")
				(action-button common/EDIT_CHOICE_PROMPT "Edit choices")
				(action-button common/DELETE_TARGET "Delete")
			]
		]
	)
)

(defn body [session contents]
	[:div {:id "outer"}
		[:h1 "Multiplex Signup Sheet"] 
		[:div {:id "contents"} (contents session)]
	]
)

(defn message-body [message]
	[:div {:id "outer"}
		[:h1 "Multiplex Signup Sheet"] 
		[:div {:id "contents"} message]
	]
)

(defn base-page [message session]
	{
		:session session
		:body (page/html5 head (message-body message))
	}
)

(defn error-page [message]
	(base-page message nil)
)

(defn new-page []
	(let [session (common/make-session)]
		{
			:session session
			:body (page/html5 head (body session identity-form))
		}
	)
)

; (defn page [db-name key]
; 	(let [session (common/load-session db-name key)]
; 		{
; 			:session session
; 			:body (page/html5 head (body session prompt-contents))
; 		}
; 	)
; )

; db-name is here because the execute-if-verified call includes it

(defn call-edit-prompt [db-name session]
	(prompt-contents session common/EDIT_CHOICE_TARGET)
)

(defn do-edit-prompt [db-name session]
	(fragment (prompt-contents session common/EDIT_CHOICE_TARGET) session)
)

(defn edit-prompt [db-name session]
	(common/execute-if-verified db-name session do-edit-prompt)
)

; db-name is here because the execute-if-verified call includes it

(defn do-identity-prompt [db-name session]
	(identity-form session)
)

(defn identity-prompt [db-name session]
	(common/execute-if-verified db-name session do-identity-prompt)
)

; db-name is here because the execute-if-verified call includes it

(defn call-address-prompt [db-name session]
	(address-form common/EDIT_ADDRESS_VERIFY (get session :address))
)

(defn do-address-prompt [db-name session]
	(fragment (address-form common/EDIT_ADDRESS_VERIFY (get session :address)) session)
)

(defn address-prompt [db-name session]
	(common/execute-if-verified db-name session do-address-prompt)
)

; db-name is here because the execute-if-verified call includes it

(defn call-name-prompt [db-name session]
	(name-form common/EDIT_NAME_TARGET (get session :name))
)

(defn do-name-prompt [db-name session]
	(fragment (name-form common/EDIT_NAME_TARGET (get session :name)) session)
)

(defn name-prompt [db-name session]
	(common/execute-if-verified db-name session do-name-prompt)
)

(defn do-display [db-name session]
	(display-contents session)
)

(defn not-implemented []
	(error-page "Not implemented")
)

(defn error-output [display result]
	(println display)
	(error-fragment result)
)

(defn signon [action]
	(base-page (address-form action "") nil)
)

(defn signon-confirm [db-name key address action]
	(with-open [connection (common/make-connection db-name)]
		(if (common/key-checked connection address key)
			(let [load-result (common/load-session connection address)]
				(if (get load-result :success)
					(let
						[
							session (get load-result :session)
							verified-session (assoc session :verified true)
							button (confirm-email action)
							confirm-form [:form button]
							transfer [:div confirm-form]
						]
						(base-page transfer verified-session)
					)
					(error-page "Session load failure")
				)
			)
			(error-page "Key mismatch")
		)
	)
)

