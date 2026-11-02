ALTER TABLE lots ADD COLUMN highest_bid_at TIMESTAMPTZ;

CREATE INDEX idx_bids_lot_id ON bids(lot_id);
