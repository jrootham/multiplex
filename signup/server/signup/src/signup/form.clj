(ns signup.form
	(:gen-class)
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.form :as form])
	(:require [signup.common :as common])
	(:require [signup.location :as location])
)


(def BEDROOM_VALUES (range 1 6))
(def BATHROOM_VALUES (range 1 4))
(def SPOT_VALUES (range 0 4))
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

(defn rent-string [bedrooms bathrooms spots size]
	(let
		[
			rent
				(+ 
					(* common/BEDROOM-COST bedrooms) 
					(* common/BATHROOM-COST bathrooms) 
					(* common/SIZE-COST size) 
					(* common/SPOT-COST spots) 
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

(defn email [address]
	[:div (form/email-field {:required "true"} "address" address)]
;	[:div "<input type=\"email\" id=\"email\" size=\"30\" required />"]
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

(defn pick-spots [spots]
	[:div {:class "choose"}
		[:fieldset 
			[:legend "Select number of parking spots"]
			(map (make-radio-map "spots" simple-label spots) SPOT_VALUES)
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

(defn location-docs []
	[:div {:class "docdiv"}
		[:div {:class "doctext"}
			(str
				"The map below is for you to indicate your preferences about where you want to live in the city. "
				"The city is divided into a 10x8 grid of tiles. "
				"The background of each tile is coloured according to how much you indicated "
				"that you want to live there."
			)
		]
		[:div {:class "legendbox"}
			[:div {:class "legend"} "0: I don't want to live there "]
			[:div {:class "outersample"} [:div {:class "sample"}]]
			[:div {:class "buffer"}]
			[:div {:class "legend"} "1: I would prefer not to live there "] 
			[:div {:class "outersample"} [:div {:class "sample mode1"}]]
			[:div {:class "buffer"}]
			[:div {:class "legend"} "2: I would be fine with living there "] 
			[:div {:class "outersample"} [:div {:class "sample mode2"}]]
			[:div {:class "buffer"}]
			[:div {:class "legend"} "3: I really want to live there "]
			[:div {:class "outersample"} [:div {:class "sample mode3"}]]
			[:div {:class "buffer"}]
		]

		[:div {:class "doctext"}
			(str
				"Each tile starts at level 0. "
				"Clicking on a tile adds one to the level, unless it is at level 3, in which case it goes back to level 0."
			)
		]
	]

)

(defn form-contents [session]
	(let 
		[
			{
				address :address
				bedrooms :bedrooms
				bathrooms :bathrooms 
				spots :spots
				size :size
				locations :locations
			} session
		]
		[:div
			[:div {:id "message"} ""]
			[:div {:id "costs"} (rent-string bedrooms bathrooms spots size)]

			[:form {:hx-post "/multiplex/server/address" :hx-trigger "change" :hx-target "#message"}
				(email address)
			]

			[:form {:hx-post "/multiplex/server/update" :hx-trigger "change" :hx-target "#costs"}
				(pick-bedrooms bedrooms)
				(pick-bathrooms bathrooms)
				(pick-spots spots)
				(pick-size size)
			]

			(location-docs)
			(location/location-map session location/tile)

;			(form/submit-button {:hx-post "/multiplex/server/reload" :hx-target "#contents"} "Reload")
			(form/submit-button {:hx-post "/multiplex/server/signup" :hx-target "#contents"} "Submit")

		]
	)
)

(defn display-contents [session]
	(let 
		[
			{
				address :address
				bedrooms :bedrooms
				bathrooms :bathrooms 
				spots :spots
				size :size
				locations :locations
			} session
		]
		[:div
				[:div {:id "costs"} (rent-string bedrooms bathrooms spots size)]
				[:div address]
				[:div 
					(format 
						"Bedrooms %d bathrooms %d parking spots %d size %d sq m (%,d sq ft)"
						bedrooms bathrooms spots size (* 10 size)
					)
				]
				[:div (location/location-map session location/display-tile)]
		]
	)
)


(defn body [session contents]
	[:div {:id "outer"}
		[:h1 "Multiplex Signup Sheet"] 
		[:div {:id "contents"} (contents session)]
	]
)

(defn new-page []
	(let [session (common/make-session)]
		{
			:session session
			:body (page/html5 head (body session form-contents))
		}
	)
)

(defn page [key]
	(let [session (common/make-session)]
		{
			:session session
			:body (page/html5 head (body session form-contents))
		}
	)
)

(defn verify [key]
	(let [session (common/make-session)]
		{
			:session session
			:body (page/html5 head (body session display-contents))
		}
	)
)
