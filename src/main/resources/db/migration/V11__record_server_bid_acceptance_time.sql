ALTER TABLE lots ADD COLUMN highest_bid_accepted_at TIMESTAMPTZ;

ALTER TABLE bids ADD COLUMN accepted_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp();
