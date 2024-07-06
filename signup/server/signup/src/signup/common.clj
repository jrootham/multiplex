(ns signup.common
	(:gen-class)
)

; Default values

(def BEDROOMS 1)
(def BATHROOMS 1)
(def SPOTS 0)
(def SIZE 50)

; Map values

(def MAP_WIDTH 2)
(def MAP_HEIGHT 2)
(def MAP_DEPTH 2)

; cost values

(def BEDROOM-COST (* 1000 0.003) )
(def BATHROOM-COST (* 10000 0.003)) 
(def SIZE-COST (* 1000 0.003)) 
(def SPOT-COST 300) 
(def KITCHEN-COST (* 10000 0.003))
(def LOAN-MONTHS 8)

(defn make-locations [width height]
	(vec (repeat height (vec (repeat width 0))))
)

(defn make-session []
	{
		:bedrooms BEDROOMS
		:bathrooms BATHROOMS
		:spots SPOTS
		:size SIZE
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

