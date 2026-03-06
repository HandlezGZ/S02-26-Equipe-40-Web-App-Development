ALTER TABLE payments
    ADD CONSTRAINT uk_payments_stripe_event_id UNIQUE (stripe_event_id);