(ns signup.location
	(:gen-class)
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.form :as form])
	(:require [hiccup.element :as element])
	(:require [signup.common :as common])
)

(defn get-value [session x y]
	(get-in session [:locations x y])
)

(defn file-name [x y value]
	(str "tiles/" x "_" y "_" value ".png")
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
					:src (file-name x y value) 
				}
			] 
			(form/hidden-field "x" x)
			(form/hidden-field "y" y)
		]
	)
)

(defn display-tile [session x y]
	(element/image x y (get-value session x y))
)

(defn location-map [session tile-fn]
	(let 
		[
			outer-fn 
				(fn [y]
					(let 
						[
							inner-fn 
								(fn [x] 
									(tile-fn session x y)
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

