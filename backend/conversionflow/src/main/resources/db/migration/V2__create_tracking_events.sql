CREATE TABLE tracking_events (
                                 id BIGSERIAL PRIMARY KEY,
                                 event_type VARCHAR(50) NOT NULL,
                                 lead_id BIGINT NOT NULL,
                                 payment_id BIGINT,
                                 metadata TEXT,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 CONSTRAINT fk_tracking_lead FOREIGN KEY (lead_id)
                                     REFERENCES leads(id)
                                     ON DELETE CASCADE
);

CREATE INDEX idx_tracking_lead ON tracking_events(lead_id);
CREATE INDEX idx_tracking_event_type ON tracking_events(event_type);