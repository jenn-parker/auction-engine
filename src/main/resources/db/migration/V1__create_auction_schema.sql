CREATE TABLE customers (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200),
    email VARCHAR(320),
    phone VARCHAR(50),
    account_status VARCHAR(20) NOT NULL,
    bidding_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_methods (
    id VARCHAR(100) PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL REFERENCES customers(id),
    method_type VARCHAR(30) NOT NULL,
    authorized BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lots (
    id VARCHAR(100) PRIMARY KEY,
    closes_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    highest_bid_amount NUMERIC(19, 4),
    highest_bidder_id VARCHAR(100) REFERENCES customers(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bids (
    id UUID PRIMARY KEY,
    lot_id VARCHAR(100) NOT NULL REFERENCES lots(id),
    customer_id VARCHAR(100) NOT NULL REFERENCES customers(id),
    amount NUMERIC(19, 4) NOT NULL,
    placed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bids_lot_placed_at ON bids(lot_id, placed_at DESC);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    lot_id VARCHAR(100) NOT NULL UNIQUE REFERENCES lots(id),
    customer_id VARCHAR(100) NOT NULL REFERENCES customers(id),
    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    gateway_reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
