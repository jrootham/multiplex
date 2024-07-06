(ns signup.common
	(:gen-class)
)

; Default values

(def BEDROOMS 1)
(def BATHROOMS 1)
(def SPOTS 0)
(def SIZE 50)

; cost values

(def BEDROOM-COST (* 1000 0.003) )
(def BATHROOM-COST (* 10000 0.003)) 
(def SIZE-COST (* 1000 0.003)) 
(def SPOT-COST 300) 
(def KITCHEN-COST (* 10000 0.003))
(def LOAN-MONTHS 8)
