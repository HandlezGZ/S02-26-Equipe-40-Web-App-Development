package com.nocountry.conversionflow.conversionflow_api.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_stripe_event",
                        columnNames = "stripe_event_id"
                )
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID do evento do Stripe (garante idempotência)
     */
    @Column(name = "stripe_event_id", nullable = false, updatable = false)
    private String stripeEventId;

    /**
     * ID da sessão de checkout do Stripe
     */
    @Column(name = "stripe_session_id", nullable = false)
    private String stripeSessionId;

    /**
     * Valor total pago (em centavos)
     */
    @Column(nullable = false)
    private Long amount;

    /**
     * Moeda (ex: usd, brl)
     */
    @Column(nullable = false)
    private String currency;

    /**
     * Status do pagamento (PAID, FAILED, REFUNDED, etc.)
     */
    @Column(nullable = false)
    private String status;

    /**
     * Data/hora da confirmação do pagamento
     */
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    // ==========================
    // GETTERS & SETTERS
    // ==========================

    public Long getId() {
        return id;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public void setStripeEventId(String stripeEventId) {
        this.stripeEventId = stripeEventId;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
