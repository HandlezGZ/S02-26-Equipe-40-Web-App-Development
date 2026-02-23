package com.nocountry.conversionflow.conversionflow_api.domain.entity;

import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_event", columnNames = "stripe_event_id"),
                @UniqueConstraint(name = "uk_payment_transaction", columnNames = "transaction_id")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stripe_event_id", nullable = false)
    private String stripeEventId;

    @Column(name = "stripe_session_id", nullable = false)
    private String stripeSessionId;

    /**
     * Use payment_intent_id aqui (idempotência por transação)
     */
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    /**
     * Valor em centavos (alinhado ao SQL BIGINT)
     */
    @Column(name = "amount", nullable = false)
    private Long amountCents;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }

    public String getStripeEventId() { return stripeEventId; }
    public void setStripeEventId(String stripeEventId) { this.stripeEventId = stripeEventId; }

    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Lead getLead() { return lead; }
    public void setLead(Lead lead) { this.lead = lead; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}