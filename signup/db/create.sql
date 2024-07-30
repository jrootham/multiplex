CREATE TABLE applicant
(
	id SERIAL PRIMARY KEY,
	magic_key VARCHAR(16) DEFAULT NULL,
	verified BOOLEAN DEFAULT false,
	verified_at TIMESTAMP DEFAULT NULL,
	address VARCHAR(128),
	bedrooms INTEGER,
	bathrooms INTEGER,
	parking INTEGER,
	size INTEGER,
	locations JSON
);
