(ns signup.location
	(:gen-class)
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.form :as form])
	(:require [signup.common :as common])
)

(defn get-value [session x y]
	(get-in session [:locations x y])
)

(defn tile [session x y]
	(let 
		[
			value (get-value session x y)
		]
		[:form 
			{
				:class "tile"
				:hx-post "/multiplex/server/location"
				:hx-target "this"
			} 
			[:input 
				{
					:type "image" 
					:src (str "tiles/" x y value ".png") 
				}
			] 
			(form/hidden-field "x" x)
			(form/hidden-field "y" y)
		]
	)
)

(defn location-map [session]
	(let 
		[
			outer-fn 
				(fn [y]
					(let 
						[
							inner-fn 
								(fn [x] 
									(tile session x y)
								)
						]
						[:div {:class "row"} (map inner-fn (range 0 common/MAP_WIDTH))]
					)
				)
		]
		(map outer-fn (range 0 common/MAP_HEIGHT))
	)
)

(defn update-location [session x-str y-str]
	(let
		[
			x (Integer/parseInt x-str)
			y (Integer/parseInt y-str)
			state (get-value session x y)
			new-session (assoc-in session [:locations x y] (mod (+ state 1) common/MAP_HEIGHT))
		]
		{
			:session new-session
			:body (str (hiccup/html (tile new-session x y)))
		}
	)
)

