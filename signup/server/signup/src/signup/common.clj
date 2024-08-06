(ns signup.common
	(:gen-class)
	(:require [crypto.random :as random])
	(:require [next.jdbc :as jdbc])
	(:require [cheshire.core :as json])
	(:require [signup.stuff :as stuff])
)

; Default values

(def BEDROOMS 1)
(def BATHROOMS 1)
(def PARKING 0)
(def SIZE 50)

; Map values

(def MAP_WIDTH 20)
(def MAP_HEIGHT 8)
(def MAP_DEPTH 4)

; cost values

(def BEDROOM-COST 300)
(def BATHROOM-COST 500) 
(def SIZE-COST 8) 
(def PARKING-COST 300) 
(def KITCHEN-COST 1000)
(def LOAN-MONTHS 8)


(def MAIL_SUBJECT "Co-op verify email")
(def KEY_SIZE 8)
(def VERIFY_URL "https://jrootham.ca/multiplex/server/verify.html")

(defn make-locations [width height]
	(vec (repeat width (vec (repeat height 0))))
)

(defn get-state [session x y]
	(get-in session [:locations x y])
)

(defn set-state [session x y state]
	(assoc-in session [:locations x y] state)
)

(defn make-connection [db-name]
	(jdbc/get-connection (assoc stuff/db :dbname db-name))
)


(defn make-session []
	{
		:verified false
		:name ""
		:address ""
		:bedrooms BEDROOMS
		:bathrooms BATHROOMS
		:parking PARKING
		:size SIZE
		:locations (make-locations MAP_WIDTH MAP_HEIGHT)
	}
)

(defn fill-session [name address bedrooms bathrooms parking size locations]
	{
		:verified false
		:name name
		:address address
		:bedrooms bedrooms
		:bathrooms bathrooms
		:parking parking
		:size size
		:locations locations
	}
)

(defn update-session [session bedrooms bathrooms parking size]
	(-> session
		(assoc :bedrooms bedrooms)
		(assoc :bathrooms bathrooms)
		(assoc :parking parking)
		(assoc :size size)
	)
)

(defn load-session [connection key]
	(let 
		[
			column-list "id,name,address,bedrooms,bathrooms,parking,size,CAST(locations AS TEXT)"
			where "magic_key=?"
			sql (str "SELECT " column-list " FROM applicant WHERE " where ";")
			statement [sql key]
		]
		(let 
			[
				result (jdbc/execute-one! connection statement)
				id (get result :applicant/id)
				name (get result :applicant/name)
				address (get result :applicant/address)
				bedrooms (get result :applicant/bedrooms)
				bathrooms (get result :applicant/bathrooms)
				parking (get result :applicant/parking)
				size (get result :applicant/size)
				locations (vec (json/parse-string (get result :locations)))
			]
			{
				:success true
				:message ""
				:id id
				:session (fill-session name address bedrooms bathrooms parking size locations)
			}
		)
	)
)

(defn mark-verified [connection id]	
	(let 
		[
			sql "UPDATE applicant SET magic_key=NULL,verified=TRUE,verified_at=NOW() WHERE id = ?;"
			result (jdbc/execute-one! connection[sql id])
		]
		(= 1 (get result :next.jdbc/update-count))	
	)
)

(defn make-key []
	(random/hex KEY_SIZE)
)

