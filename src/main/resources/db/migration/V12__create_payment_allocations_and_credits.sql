CREATE TABLE payment_allocations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    payment_method_id VARCHAR(100),
    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE customer_credit_ledger (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL REFERENCES customers(id),
    amount NUMERIC(19, 4) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    reference_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_credit_ledger_customer ON customer_credit_ledger(customer_id, created_at);
