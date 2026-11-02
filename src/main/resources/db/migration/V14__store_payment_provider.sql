ALTER TABLE payment_methods ADD COLUMN provider VARCHAR(100) NOT NULL DEFAULT 'external';
