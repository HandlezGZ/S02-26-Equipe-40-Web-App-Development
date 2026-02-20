package com.nocountry.conversionflow.conversionflow_api.service;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.LeadStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import com.nocountry.conversionflow.conversionflow_api.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.repository.PaymentRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final LeadRepository leadRepository;
    private final TrackingService trackingService;

    public StripeWebhookService(PaymentRepository paymentRepository,
                                LeadRepository leadRepository,
                                TrackingService trackingService) {
        this.paymentRepository = paymentRepository;
        this.leadRepository = leadRepository;
        this.trackingService = trackingService;
    }

    @Transactional
    public void processEvent(String payload, String sigHeader, String endpointSecret) {

        Event event = constructEvent(payload, sigHeader, endpointSecret);

        log.info("Stripe event received: {}", event.getType());

        // 🔒 Idempotência
        if (paymentRepository.existsByStripeEventId(event.getId())) {
            log.info("Event already processed: {}", event.getId());
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private Event constructEvent(String payload, String sigHeader, String endpointSecret) {
        try {
            return Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature", e);
            throw new RuntimeException("Invalid webhook signature");
        } catch (Exception e) {
            log.error("Unexpected error while constructing Stripe event", e);
            throw new RuntimeException("Webhook processing failed");
        }
    }

    private void handleCheckoutCompleted(Event event) {

        Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();

        if (stripeObject.isEmpty() || !(stripeObject.get() instanceof Session session)) {
            throw new RuntimeException("Invalid Stripe session object");
        }

        validateSession(session);

        Long leadId = Long.parseLong(session.getMetadata().get("leadId"));

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found: " + leadId));

        Payment payment = createPayment(event, session, lead);

        updateLeadStatusIfNecessary(lead);

        trackingService.processSuccessfulPayment(payment);

        log.info("Payment processed successfully for lead {}", lead.getId());
    }

    private void validateSession(Session session) {

        if (session.getMetadata() == null || session.getMetadata().get("leadId") == null) {
            throw new RuntimeException("LeadId not found in Stripe metadata");
        }

        if (session.getAmountTotal() == null || session.getCurrency() == null) {
            throw new RuntimeException("Invalid payment data from Stripe");
        }
    }

    private Payment createPayment(Event event, Session session, Lead lead) {

        Payment payment = new Payment();
        payment.setStripeEventId(event.getId());
        payment.setStripeSessionId(session.getId());
        payment.setAmount(session.getAmountTotal());
        payment.setCurrency(session.getCurrency());
        payment.setStatus(PaymentStatus.PAID);
        payment.setLead(lead);
        payment.setCreatedAt(OffsetDateTime.now());

        return paymentRepository.save(payment);
    }

    private void updateLeadStatusIfNecessary(Lead lead) {

        if (lead.getStatus() == LeadStatus.WON || lead.getStatus() == LeadStatus.LOST) {
            return;
        }

        lead.setStatus(LeadStatus.WON);
        leadRepository.save(lead);
    }
}