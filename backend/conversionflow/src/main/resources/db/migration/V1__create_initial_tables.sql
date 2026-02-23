CREATE TABLE leads (
                       id BIGSERIAL PRIMARY KEY,
                       external_id VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       status VARCHAR(50) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
                          stripe_session_id VARCHAR(255) NOT NULL,
                          amount BIGINT NOT NULL,
                          currency VARCHAR(10) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          lead_id BIGINT NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_payment_lead
                              FOREIGN KEY (lead_id)
                                  REFERENCES leads(id)
                                  ON DELETE CASCADE
);

CREATE TABLE conversions (
                             id BIGSERIAL PRIMARY KEY,
                             lead_id BIGINT NOT NULL,
                             plan VARCHAR(255) NOT NULL,
                             status VARCHAR(50) NOT NULL,
                             payment_id BIGINT,
                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                             CONSTRAINT fk_conversion_lead
                                 FOREIGN KEY (lead_id)
                                     REFERENCES leads(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_conversion_payment
                                 FOREIGN KEY (payment_id)
                                     REFERENCES payments(id)
                                     ON DELETE SET NULL
);
