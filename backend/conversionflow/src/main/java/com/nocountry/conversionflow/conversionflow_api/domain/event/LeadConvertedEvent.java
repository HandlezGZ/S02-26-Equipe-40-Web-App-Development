package com.nocountry.conversionflow.conversionflow_api.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Evento de domínio emitido quando um Lead vira WON (pagamento confirmado).
 *
 * Observação: este evento é serializado em JSON e persistido em ConversionDispatch.payload,
 * então ele precisa ser compatível com Jackson (JsonCreator + JsonProperty).
 */
public class LeadConvertedEvent {

    private final Long leadId;
    private final String externalId;
    private final String email;

    private final String gclid;
    private final String fbclid;
    private final String fbp;
    private final String fbc;

    /**
     * Para dedupe: event_id = paymentIntentId (Meta) e chave única interna também.
     */
    private final String paymentIntentId;

    private final BigDecimal convertedAmount;
    private final String currency;
    private final OffsetDateTime convertedAt;

    @JsonCreator
    public LeadConvertedEvent(
            @JsonProperty("leadId") Long leadId,
            @JsonProperty("externalId") String externalId,
            @JsonProperty("email") String email,
            @JsonProperty("gclid") String gclid,
            @JsonProperty("fbclid") String fbclid,
            @JsonProperty("fbp") String fbp,
            @JsonProperty("fbc") String fbc,
            @JsonProperty("paymentIntentId") String paymentIntentId,
            @JsonProperty("convertedAmount") BigDecimal convertedAmount,
            @JsonProperty("currency") String currency,
            @JsonProperty("convertedAt") OffsetDateTime convertedAt
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
        this.currency = currency;
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
    public String getCurrency() { return currency; }
    public OffsetDateTime getConvertedAt() { return convertedAt; }
}
