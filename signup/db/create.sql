CREATE TABLE applicant
(
	id SERIAL PRIMARY KEY,
	created_at TIMESTAMP DEFAULT NOW(),
	magic_key VARCHAR(16) DEFAULT NULL,
	verified BOOLEAN DEFAULT FALSE,
	verified_at TIMESTAMP DEFAULT NULL,
	address VARCHAR(254),
	name VARCHAR(256),
	active BOOLEAN DEFAULT TRUE,
	bedrooms INTEGER,
	bathrooms INTEGER,
	parking INTEGER,
	size INTEGER,
	locations JSON
);
