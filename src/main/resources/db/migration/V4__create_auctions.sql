CREATE TABLE auctions (
    id VARCHAR(100) PRIMARY KEY,
    scheduled_at TIMESTAMPTZ NOT NULL,
    venue_name VARCHAR(200),
    location_line1 VARCHAR(200),
    location_line2 VARCHAR(200),
    location_city VARCHAR(100),
    location_state_or_region VARCHAR(100),
    location_postal_code VARCHAR(30),
    location_country_code VARCHAR(2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE lots ADD COLUMN auction_id VARCHAR(100);
ALTER TABLE lots ADD CONSTRAINT fk_lots_auction FOREIGN KEY (auction_id) REFERENCES auctions(id);
CREATE INDEX idx_lots_auction_id ON lots(auction_id);
