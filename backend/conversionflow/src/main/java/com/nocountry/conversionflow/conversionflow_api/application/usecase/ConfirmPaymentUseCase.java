package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.PaymentRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.service.LeadConversionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class ConfirmPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmPaymentUseCase.class);

    private final StripeProperties stripeProperties;
    private final LeadRepository leadRepository;
    private final PaymentRepository paymentRepository;
    private final LeadConversionService leadConversionService;
    private final ApplicationEventPublisher eventPublisher;

    public ConfirmPaymentUseCase(
            StripeProperties stripeProperties,
            LeadRepository leadRepository,
            PaymentRepository paymentRepository,
            LeadConversionService leadConversionService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.stripeProperties = stripeProperties;
        this.leadRepository = leadRepository;
        this.paymentRepository = paymentRepository;
        this.leadConversionService = leadConversionService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(String payload, String signatureHeader) {
        log.info("usecase.confirmPayment.start payloadSize={}", payload == null ? 0 : payload.length());

        Event event = constructStripeEvent(payload, signatureHeader);
        log.info("usecase.confirmPayment.eventParsed eventType={} eventId={}", event.getType(), event.getId());

        if (!"checkout.session.completed".equals(event.getType())) {
            log.info("usecase.confirmPayment.ignored eventType={} eventId={}", event.getType(), event.getId());
            return;
        }

        Session session = extractSession(event);

        String leadIdStr = getMetadata(session, "leadId");
        if (leadIdStr == null) {
            log.warn("usecase.confirmPayment.missingLeadIdMetadata sessionId={}", session.getId());
            return;
        }

        Long leadId;
        try {
            leadId = Long.valueOf(leadIdStr);
        } catch (Exception e) {
            log.warn("usecase.confirmPayment.invalidLeadIdMetadata leadId={} sessionId={}", leadIdStr, session.getId());
            return;
        }

        Lead lead = leadRepository.findById(leadId).orElse(null);
        if (lead == null) {
            log.warn("usecase.confirmPayment.leadNotFound leadId={}", leadId);
            return;
        }

        String stripeEventId = event.getId();
        String stripeSessionId = session.getId();
        String paymentIntentId = session.getPaymentIntent();

        if (stripeEventId != null && paymentRepository.existsByStripeEventId(stripeEventId)) {
            log.info("usecase.confirmPayment.duplicateEvent stripeEventId={}", stripeEventId);
            return;
        }
        if (paymentIntentId != null && paymentRepository.existsByTransactionId(paymentIntentId)) {
            log.info("usecase.confirmPayment.duplicatePaymentIntent paymentIntentId={}", paymentIntentId);
            return;
        }

        PaymentIntent paymentIntent = retrievePaymentIntent(paymentIntentId);

        long amountCents = paymentIntent.getAmount() == null ? 0L : paymentIntent.getAmount();
        String currency = paymentIntent.getCurrency() == null ? stripeProperties.getCurrency() : paymentIntent.getCurrency();
        BigDecimal amount = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));

        Payment payment = new Payment();
        payment.setStripeEventId(stripeEventId);
        payment.setStripeSessionId(stripeSessionId);
        payment.setTransactionId(paymentIntentId);
        payment.setAmountCents(amountCents);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PAID);
        payment.setLead(lead);
        payment.setCreatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        if (leadConversionService.markAsWon(lead, amount)) {
            leadRepository.save(lead);
            log.info("usecase.confirmPayment.leadUpdated leadId={} status={} convertedAmount={}",
                    lead.getId(), lead.getStatus(), lead.getConvertedAmount());
        }

        publishLeadConvertedEvent(lead, paymentIntentId);
        log.info("usecase.confirmPayment.success leadId={} paymentIntentId={}", lead.getId(), paymentIntentId);
    }

    private Event constructStripeEvent(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("usecase.confirmPayment.invalidSignature", e);
            throw new RuntimeException("Invalid Stripe signature", e);
        } catch (Exception e) {
            log.error("usecase.confirmPayment.parseError", e);
            throw new RuntimeException("Error parsing Stripe event", e);
        }
    }

    private Session extractSession(Event event) {
        return (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Invalid session object"));
    }

    private String getMetadata(Session session, String key) {
        if (session.getMetadata() == null) return null;
        String value = session.getMetadata().get(key);
        if (value == null || value.isBlank()) return null;
        return value;
    }

    private PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalStateException("Missing paymentIntentId in checkout session");
        }
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("usecase.confirmPayment.paymentIntentRetrieveError paymentIntentId={}", paymentIntentId, e);
            throw new RuntimeException("Error retrieving PaymentIntent", e);
        }
    }

    private void publishLeadConvertedEvent(Lead lead, String paymentIntentId) {
        LeadConvertedEvent event = new LeadConvertedEvent(
                lead.getId(),
                lead.getExternalId(),
                lead.getEmail(),
                lead.getGclid(),
                lead.getFbclid(),
                lead.getFbp(),
                lead.getFbc(),
                paymentIntentId,
                lead.getConvertedAmount(),
                lead.getConvertedAt()
        );

        eventPublisher.publishEvent(event);
    }
}
