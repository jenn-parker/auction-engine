CREATE TABLE payments (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL REFERENCES customers(id),
    payment_method_id VARCHAR(100) NOT NULL REFERENCES payment_methods(id),
    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_customer_created ON payments(customer_id, created_at DESC);
ALTER TABLE payment_allocations ADD COLUMN payment_id UUID REFERENCES payments(id);
