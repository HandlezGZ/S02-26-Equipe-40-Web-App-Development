package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.PaymentRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.service.LeadConversionService;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe.StripeClient;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe.StripePaymentIntentSnapshot;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe.StripeSessionSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class CapturePixelEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(CapturePixelEventUseCase.class);

    private final LeadRepository leadRepository;
    private final PaymentRepository paymentRepository;
    private final LeadConversionService leadConversionService;
    private final StripeProperties stripeProperties;
    private final StripeClient stripeClient;
    private final ApplicationEventPublisher eventPublisher;

    public CapturePixelEventUseCase(
            LeadRepository leadRepository,
            PaymentRepository paymentRepository,
            LeadConversionService leadConversionService,
            StripeProperties stripeProperties,
            StripeClient stripeClient,
            ApplicationEventPublisher eventPublisher
    ) {
        this.leadRepository = leadRepository;
        this.paymentRepository = paymentRepository;
        this.leadConversionService = leadConversionService;
        this.stripeProperties = stripeProperties;
        this.stripeClient = stripeClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(
            Long leadId,
            String externalId,
            String checkoutSessionId,
            String gclid,
            String fbclid,
            String fbp,
            String fbc,
            String utmSource,
            String utmCampaign
    ) {
        if (leadId == null && (externalId == null || externalId.isBlank())) {
            throw new IllegalArgumentException("leadId or externalId is required");
        }

        Lead lead = resolveLead(leadId, externalId);
        lead.updateTracking(gclid, fbclid, fbp, fbc, utmSource, utmCampaign);
        leadRepository.save(lead);
        confirmPurchaseFromSessionIfProvided(lead, checkoutSessionId);

        log.info("usecase.pixelEvent.capture.success leadId={} externalId={} checkoutSessionIdProvided={} gclid={} fbclid={} fbp={} fbc={} utmSource={} utmCampaign={}",
                lead.getId(),
                lead.getExternalId(),
                blankToNull(checkoutSessionId) != null,
                blankToNull(gclid) != null,
                blankToNull(fbclid) != null,
                blankToNull(fbp) != null,
                blankToNull(fbc) != null,
                blankToNull(utmSource) != null,
                blankToNull(utmCampaign) != null);
    }

    private void confirmPurchaseFromSessionIfProvided(Lead lead, String checkoutSessionId) {
        String normalizedSessionId = blankToNull(checkoutSessionId);
        if (normalizedSessionId == null) {
            return;
        }

        try {
            StripeSessionSnapshot session = stripeClient.retrieveSession(normalizedSessionId);
            if (!"paid".equalsIgnoreCase(session.paymentStatus())) {
                log.info("usecase.pixelEvent.capture.notPaidYet leadId={} checkoutSessionId={} paymentStatus={}",
                        lead.getId(), normalizedSessionId, session.paymentStatus());
                return;
            }

            String metadataLeadId = blankToNull(session.leadIdMetadata());
            if (metadataLeadId != null && !metadataLeadId.isBlank() && !String.valueOf(lead.getId()).equals(metadataLeadId)) {
                log.warn("usecase.pixelEvent.capture.metadataLeadMismatch leadId={} metadataLeadId={} checkoutSessionId={}",
                        lead.getId(), metadataLeadId, normalizedSessionId);
                return;
            }

            String paymentIntentId = blankToNull(session.paymentIntentId());
            if (paymentIntentId == null) {
                log.warn("usecase.pixelEvent.capture.missingPaymentIntent leadId={} checkoutSessionId={}",
                        lead.getId(), normalizedSessionId);
                return;
            }

            StripePaymentIntentSnapshot paymentIntent = stripeClient.retrievePaymentIntent(paymentIntentId);
            long amountCents = paymentIntent.amountCents() == null ? 0L : paymentIntent.amountCents();
            String currency = blankToNull(paymentIntent.currency()) == null
                    ? stripeProperties.getCurrency()
                    : paymentIntent.currency();
            BigDecimal amount = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));

            if (!paymentRepository.existsByTransactionId(paymentIntentId)) {
                Payment payment = new Payment();
                payment.setStripeEventId("pixel-session:" + normalizedSessionId);
                payment.setStripeSessionId(normalizedSessionId);
                payment.setTransactionId(paymentIntentId);
                payment.setAmountCents(amountCents);
                payment.setCurrency(currency);
                payment.setStatus(PaymentStatus.PAID);
                payment.setLead(lead);
                payment.setCreatedAt(OffsetDateTime.now());
                paymentRepository.save(payment);
            }

            if (leadConversionService.markAsWon(lead, amount)) {
                leadRepository.save(lead);
                publishLeadConvertedEvent(lead, paymentIntentId);
                log.info("usecase.pixelEvent.capture.markedWon leadId={} checkoutSessionId={} paymentIntentId={}",
                        lead.getId(), normalizedSessionId, paymentIntentId);
            }
        } catch (RuntimeException e) {
            log.error("usecase.pixelEvent.capture.sessionConfirmError leadId={} checkoutSessionId={}",
                    lead.getId(), normalizedSessionId, e);
        }
    }

    private Lead resolveLead(Long leadId, String externalId) {
        if (leadId != null) {
            return leadRepository.findById(leadId)
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));
        }

        return leadRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found for externalId: " + externalId));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
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
