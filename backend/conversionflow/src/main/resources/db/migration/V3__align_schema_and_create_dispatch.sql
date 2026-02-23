-- ===== Leads: tracking + conversion fields =====
ALTER TABLE leads ADD COLUMN IF NOT EXISTS gclid VARCHAR(255);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS fbclid VARCHAR(255);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS fbp VARCHAR(255);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS fbc VARCHAR(255);

ALTER TABLE leads ADD COLUMN IF NOT EXISTS utm_source VARCHAR(255);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS utm_campaign VARCHAR(255);

ALTER TABLE leads ADD COLUMN IF NOT EXISTS converted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE leads ADD COLUMN IF NOT EXISTS converted_amount NUMERIC(15,2);

-- ===== Payments: transaction_id (payment_intent_id) =====
ALTER TABLE payments ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(255);

-- garantir NOT NULL para ambiente limpo (mvp)
UPDATE payments SET transaction_id = stripe_session_id WHERE transaction_id IS NULL;
ALTER TABLE payments ALTER COLUMN transaction_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_payment_transaction') THEN
ALTER TABLE payments ADD CONSTRAINT uk_payment_transaction UNIQUE (transaction_id);
END IF;
END $$;

-- ===== Conversion Dispatch table =====
CREATE TABLE IF NOT EXISTS conversion_dispatch (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   lead_id BIGINT NOT NULL,
                                                   provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    last_attempt_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_conversion_dispatch_status
    ON conversion_dispatch(status);

CREATE INDEX IF NOT EXISTS idx_conversion_dispatch_provider
    ON conversion_dispatch(provider);

CREATE INDEX IF NOT EXISTS idx_conversion_dispatch_lead
    ON conversion_dispatch(lead_id);