(ns signup.location
	(:gen-class)
	(:require [hiccup2.core :as hiccup])
	(:require [hiccup.page :as page])
	(:require [hiccup.form :as form])
	(:require [hiccup.element :as element])
	(:require [signup.common :as common])
)

(defn file-name [x y]
	(str "tiles/map_" y "_" x ".png")
)

(defn tile [session x y]
	(let 
		[
			value (common/get-state session x y)
		]
		[:form 
			{
				:class "tile"
				:hx-post "/multiplex/server/location"
				:hx-target "this"
				:hx-swap :outerHTML
			} 
			[:input 
				{
					:class (str "tile_image" " " "mode" value)
					:type "image" 
					:src (file-name x y) 
				}
			] 
			(form/hidden-field "x" x)
			(form/hidden-field "y" y)
		]
	)
)

(defn major-intersection [x y]
	"Major intersection"
)

(defn display-tile [session x y]
	(let 
		[value (common/get-state session x y)]
		(element/image 
			{
				:class (str "tile_image" " " "mode" value)
				:alt (major-intersection x y)
			} 
			(file-name x y)
		)
	)
)

(defn location-map-tiles [session tile-fn]
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

(defn location-map [session tile-fn]
	[:div {:class "map_container"} (location-map-tiles session tile-fn)]
)

(defn update-location [session x-str y-str]
	(let
		[
			x (Integer/parseInt x-str)
			y (Integer/parseInt y-str)
			state (common/get-state session x y)
			new-session (common/set-state session x y (mod (+ state 1) common/MAP_DEPTH))
		]
		{
			:session new-session
			:body (str (hiccup/html (tile new-session x y)))
		}
	)
)

