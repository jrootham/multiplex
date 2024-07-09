(ns signup.common
	(:gen-class)
)

; Default values

(def BEDROOMS 1)
(def BATHROOMS 1)
(def SPOTS 0)
(def SIZE 50)

; Map values

(def MAP_WIDTH 20)
(def MAP_HEIGHT 8)
(def MAP_DEPTH 4)

; cost values

(def BEDROOM-COST 300)
(def BATHROOM-COST 500) 
(def SIZE-COST 8) 
(def SPOT-COST 300) 
(def KITCHEN-COST 1000)
(def LOAN-MONTHS 8)

(defn make-locations [width height]
	(vec (repeat width (vec (repeat height 0))))
)

(defn get-state [session x y]
	(get-in session [:locations x y])
)

(defn set-state [session x y state]
	(assoc-in session [:locations x y] state)
)

(defn make-session []
	{
		:bedrooms BEDROOMS
		:bathrooms BATHROOMS
		:spots SPOTS
		:size SIZE
		:address ""
		:locations (make-locations MAP_WIDTH MAP_HEIGHT)
	}
)

(defn update-session [session bedrooms bathrooms spots size]
	(-> session
		(assoc :bedrooms bedrooms)
		(assoc :bathrooms bathrooms)
		(assoc :spots spots)
		(assoc :size size)
	)
)

