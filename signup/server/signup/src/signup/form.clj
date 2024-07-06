(ns signup.form
	(:gen-class)
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.form :as form])
	(:require [signup.common :as common])
)


(def BEDROOM_VALUES (range 1 6))
(def BATHROOM_VALUES (range 1 3))
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

(defn email []
	[:div
		[:label {:for "email"} "Enter your email:"]
		[:input {:type "email" :id "email" :size "30"}]
	]
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

(defn contents [bedrooms bathrooms spots size]
	[:form {:hx-post "/multiplex/server/update" :hx-trigger "change" :hx-target "#costs"}
		[:div {:id "costs"} (rent-string bedrooms bathrooms spots size)]
		(email)
		(pick-bedrooms bedrooms)
		(pick-bathrooms bathrooms)
		(pick-spots spots)
		(pick-size size)

		(form/submit-button {:hx-post "/multiplex/server/reload" :hx-target "#contents"} "Reload")
		(form/submit-button {:hx-post "/multiplex/server/signup" :hx-target "#contents"} "Submit")
	]
)

(defn body [bedrooms bathrooms spots size]
	[:div {:id "outer"}
		[:h1 "Multiplex Signup Sheet"] 
		[:div {:id "contents"} (contents bedrooms bathrooms spots size)]
	]
)

(defn page [key]
	(if (= key 0)
		(page/html5 head (body common/BEDROOMS common/BATHROOMS common/SPOTS common/SIZE))
		(let
			[
				bedrooms 1
				bathrooms 1
				spots 0 
				size 50
			]
			(page/html5 head (body bedrooms bathrooms spots size))
		)
	)
)