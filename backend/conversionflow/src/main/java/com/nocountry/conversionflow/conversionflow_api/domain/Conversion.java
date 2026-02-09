package com.nocountry.conversionflow.conversionflow_api.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "conversions")
public class Conversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador do lead (Webflow / frontend)
     */
    @Column(name = "lead_id", nullable = false)
    private String leadId;

    /**
     * Plano contratado (ex: INCORPORATION_BASIC)
     */
    @Column(nullable = false)
    private String plan;

    /**
     * Pagamento confirmado associado à conversão
     */
    @OneToOne(optional = false)
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_conversion_payment")
    )
    private Payment payment;

    /**
     * Data/hora da conversão
     */
    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    // ==========================
    // GETTERS & SETTERS
    // ==========================

    public Long getId() {
        return id;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
