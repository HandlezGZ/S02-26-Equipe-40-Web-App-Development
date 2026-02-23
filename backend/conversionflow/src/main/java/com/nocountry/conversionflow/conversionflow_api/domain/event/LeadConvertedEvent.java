package com.nocountry.conversionflow.conversionflow_api.domain.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LeadConvertedEvent {

    private final Long leadId;
    private final String externalId;
    private final String email;

    private final String gclid;
    private final String fbclid;
    private final String fbp;
    private final String fbc;

    /**
     * Para dedupe: event_id = paymentIntentId (Meta) e chave única interna também
     */
    private final String paymentIntentId;

    private final BigDecimal convertedAmount;
    private final OffsetDateTime convertedAt;

    public LeadConvertedEvent(
            Long leadId,
            String externalId,
            String email,
            String gclid,
            String fbclid,
            String fbp,
            String fbc,
            String paymentIntentId,
            BigDecimal convertedAmount,
            OffsetDateTime convertedAt
    ) {
        this.leadId = leadId;
        this.externalId = externalId;
        this.email = email;
        this.gclid = gclid;
        this.fbclid = fbclid;
        this.fbp = fbp;
        this.fbc = fbc;
        this.paymentIntentId = paymentIntentId;
        this.convertedAmount = convertedAmount;
        this.convertedAt = convertedAt;
    }

    public Long getLeadId() { return leadId; }
    public String getExternalId() { return externalId; }
    public String getEmail() { return email; }

    public String getGclid() { return gclid; }
    public String getFbclid() { return fbclid; }
    public String getFbp() { return fbp; }
    public String getFbc() { return fbc; }

    public String getPaymentIntentId() { return paymentIntentId; }

    public BigDecimal getConvertedAmount() { return convertedAmount; }
    public OffsetDateTime getConvertedAt() { return convertedAt; }
}