ALTER TABLE payment_methods ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uq_payment_methods_one_default_per_customer
    ON payment_methods(customer_id) WHERE is_default = TRUE;
