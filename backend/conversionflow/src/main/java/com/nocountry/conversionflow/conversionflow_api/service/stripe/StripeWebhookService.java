package com.nocountry.conversionflow.conversionflow_api.service.stripe;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.PaymentRepository;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private static final String EVENT_CHECKOUT_COMPLETED = "checkout.session.completed";
    private static final String METADATA_LEAD_ID = "leadId";
    private static final String DEFAULT_CURRENCY = "brl";

    private final StripeProperties stripeProperties;
    private final PaymentRepository paymentRepository;
    private final LeadRepository leadRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StripeWebhookService(
            StripeProperties stripeProperties,
            PaymentRepository paymentRepository,
            LeadRepository leadRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.stripeProperties = stripeProperties;
        this.paymentRepository = paymentRepository;
        this.leadRepository = leadRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Método chamado pelo controller do webhook.
     * Mantemos @Transactional aqui para garantir atomicidade (Payment + Lead) e evitar warnings de self-invocation.
     */
    @Transactional
    public void process(String payload, String sigHeader) {
        Event event = verifyAndParseEvent(payload, sigHeader);

        log.info("Stripe webhook recebido: type={} eventId={}", event.getType(), event.getId());

        if (!EVENT_CHECKOUT_COMPLETED.equals(event.getType())) {
            // Nunca quebre por evento não utilizado
            log.info("Evento Stripe ignorado: {}", event.getType());
            return;
        }

        handleCheckoutSessionCompleted(event);
    }

    private Event verifyAndParseEvent(String payload, String sigHeader) {
        String secret = stripeProperties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET não configurado");
        }
        if (sigHeader == null || sigHeader.isBlank()) {
            throw new IllegalArgumentException("Header Stripe-Signature ausente");
        }

        try {
            return Webhook.constructEvent(payload, sigHeader, secret);
        } catch (SignatureVerificationException e) {
            log.error("Assinatura inválida no webhook Stripe: {}", e.getMessage());
            throw new IllegalArgumentException("Assinatura inválida");
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        final String eventId = event.getId();

        // ✅ Idempotência por event.id
        if (paymentRepository.existsByStripeEventId(eventId)) {
            log.warn("Evento já processado (idempotência por stripe_event_id): {}", eventId);
            return;
        }

        Session session = deserializeSessionRobust(event);

        final String sessionId = safeTrim(session.getId());
        final String paymentIntentId = safeTrim(session.getPaymentIntent()); // transaction_id
        final Long amountTotalCents = session.getAmountTotal();
        final String currency = normalizeCurrency(session.getCurrency());
        final String leadIdStr = extractLeadId(session);

        log.info("checkout.session.completed: sessionId={} paymentIntentId={} amountTotalCents={} currency={} leadIdStr={}",
                sessionId, paymentIntentId, amountTotalCents, currency, leadIdStr);

        if (sessionId == null) {
            throw new IllegalStateException("session.id ausente no checkout.session.completed");
        }

        // `transaction_id` é NOT NULL no seu Payment.
        if (paymentIntentId == null) {
            throw new IllegalStateException(
                    "payment_intent ausente no checkout.session.completed (sessionId=" + sessionId + ")"
            );
        }

        // ✅ Idempotência extra por transação
        if (paymentRepository.existsByTransactionId(paymentIntentId)) {
            log.warn("Transação já registrada (idempotência por transaction_id): {}", paymentIntentId);
            return;
        }

        Lead lead = resolveLead(sessionId, leadIdStr);

        OffsetDateTime now = OffsetDateTime.now();
        BigDecimal convertedAmount = toMoney(amountTotalCents);

        // ✅ Persistir Payment
        Payment payment = new Payment();
        payment.setStripeEventId(eventId);
        payment.setStripeSessionId(sessionId);
        payment.setTransactionId(paymentIntentId);
        payment.setAmountCents(amountTotalCents != null ? amountTotalCents : 0L);
        payment.setCurrency(currency);

        // ⚠️ Se seu enum não tem SUCCEEDED, troque pelo valor existente (ex.: PAID)
        payment.setStatus(PaymentStatus.PAID);

        payment.setLead(lead);
        payment.setCreatedAt(now);

        paymentRepository.save(payment);

        // ✅ Atualizar Lead usando método de domínio
        lead.markAsWon(convertedAmount);
        leadRepository.save(lead);

        log.info("Pagamento confirmado e persistido: leadId={} paymentId={} sessionId={} paymentIntentId={}",
                lead.getId(), payment.getId(), sessionId, paymentIntentId);

        // ✅ Dispara evento de domínio para enfileirar dispatches (Meta/Google/Pipedrive) AFTER_COMMIT
        LeadConvertedEvent convertedEvent = new LeadConvertedEvent(
                lead.getId(),
                lead.getExternalId(),
                lead.getEmail(),
                lead.getGclid(),
                lead.getFbclid(),
                lead.getFbp(),
                lead.getFbc(),
                paymentIntentId,
                convertedAmount,
                currency,
                now
        );

        eventPublisher.publishEvent(convertedEvent);
    }

    private String extractLeadId(Session session) {
        String leadIdStr = null;

        if (session.getMetadata() != null) {
            leadIdStr = session.getMetadata().get(METADATA_LEAD_ID);
        }
        if (leadIdStr == null || leadIdStr.isBlank()) {
            leadIdStr = session.getClientReferenceId();
        }

        return safeTrim(leadIdStr);
    }

    private String normalizeCurrency(String currency) {
        String c = safeTrim(currency);
        if (c == null) return DEFAULT_CURRENCY;
        return c.toLowerCase();
    }

    private BigDecimal toMoney(Long amountCents) {
        if (amountCents == null) return BigDecimal.ZERO;
        // 12345 -> 123.45
        return BigDecimal.valueOf(amountCents).movePointLeft(2);
    }

    /**
     * Desserialização robusta para evitar falhas quando a API version do evento
     * não bate com o SDK do Stripe (caso comum com Stripe CLI).
     *
     * Estratégia:
     * - usar o JSON bruto de event.getData().getObject().toJson()
     * - parsear com Session.GSON (bem tolerante)
     */
    private Session deserializeSessionRobust(Event event) {
        try {
            String json = event.getData().getObject().toJson();
            Session session = Session.GSON.fromJson(json, Session.class);

            if (session == null || session.getId() == null || session.getId().isBlank()) {
                throw new IllegalArgumentException("Session inválida após desserialização manual");
            }

            return session;
        } catch (Exception e) {
            log.error("Falha ao desserializar Session do evento {} (type={}): {}",
                    event.getId(), event.getType(), e.getMessage(), e);
            throw new IllegalArgumentException("Não foi possível desserializar o objeto do evento como Session", e);
        }
    }

    private Lead resolveLead(String sessionId, String leadIdStr) {
        // 1) por leadId (metadata/client_reference_id)
        if (leadIdStr != null && !leadIdStr.isBlank()) {
            Long leadId;
            try {
                leadId = Long.valueOf(leadIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("leadId inválido no webhook: " + leadIdStr);
            }

            return leadRepository.findById(leadId)
                    .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado: " + leadId));
        }

        // 2) fallback: por external_id == sessionId (se você salvou isso no checkout)
        return leadRepository.findByExternalId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não foi possível correlacionar Lead: leadId ausente e nenhum lead com external_id=" + sessionId
                ));
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}