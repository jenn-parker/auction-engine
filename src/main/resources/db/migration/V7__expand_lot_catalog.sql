ALTER TABLE lots ADD COLUMN opens_at TIMESTAMPTZ;
ALTER TABLE lots ADD COLUMN category VARCHAR(120);
ALTER TABLE lots ADD COLUMN condition_code VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE lots ADD COLUMN condition_notes TEXT;
ALTER TABLE lots ADD COLUMN provenance_summary TEXT;
ALTER TABLE lots ADD COLUMN provenance_previous_owner VARCHAR(200);
ALTER TABLE lots ADD COLUMN provenance_documentation_reference VARCHAR(500);
ALTER TABLE lots ADD COLUMN height NUMERIC(19, 4);
ALTER TABLE lots ADD COLUMN width NUMERIC(19, 4);
ALTER TABLE lots ADD COLUMN length_value NUMERIC(19, 4);
ALTER TABLE lots ADD COLUMN depth NUMERIC(19, 4);
ALTER TABLE lots ADD COLUMN weight NUMERIC(19, 4);
ALTER TABLE lots ADD COLUMN dimension_unit VARCHAR(20);
ALTER TABLE lots ADD COLUMN weight_unit VARCHAR(20);
ALTER TABLE lots ADD COLUMN reserve_price NUMERIC(19, 4);
ALTER TABLE lots ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE lots ADD COLUMN buyer_premium_rate NUMERIC(9, 6) NOT NULL DEFAULT 0;
ALTER TABLE lots ADD COLUMN tax_rate NUMERIC(9, 6) NOT NULL DEFAULT 0;
ALTER TABLE lots ADD COLUMN winning_customer_id VARCHAR(100);
ALTER TABLE lots ADD COLUMN winning_bid_amount NUMERIC(19, 4);
ALTER TABLE lots ADD COLUMN settled_at TIMESTAMPTZ;

CREATE TABLE lot_fulfillment_options (
    lot_id VARCHAR(100) NOT NULL REFERENCES lots(id),
    option_code VARCHAR(40) NOT NULL,
    PRIMARY KEY (lot_id, option_code)
);

CREATE TABLE lot_images (
    lot_id VARCHAR(100) NOT NULL REFERENCES lots(id),
    image_url VARCHAR(1000) NOT NULL,
    PRIMARY KEY (lot_id, image_url)
);
