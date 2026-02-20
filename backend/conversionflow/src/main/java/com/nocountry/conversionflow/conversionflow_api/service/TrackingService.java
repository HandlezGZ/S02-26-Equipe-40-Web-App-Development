package com.nocountry.conversionflow.conversionflow_api.service;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Payment;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.TrackingEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.TrackingEventType;
import com.nocountry.conversionflow.conversionflow_api.repository.TrackingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class TrackingService {

    private final TrackingEventRepository trackingEventRepository;

    public TrackingService(TrackingEventRepository trackingEventRepository) {
        this.trackingEventRepository = trackingEventRepository;
    }

    @Transactional
    public void trackLeadCreated(Lead lead) {
        validateLead(lead);
        createEvent(TrackingEventType.LEAD_CREATED, lead, null, null);
    }

    @Transactional
    public void trackCheckoutStarted(Lead lead) {
        validateLead(lead);
        createEvent(TrackingEventType.CHECKOUT_STARTED, lead, null, null);
    }

    @Transactional
    public void processSuccessfulPayment(Payment payment) {
        Objects.requireNonNull(payment, "Payment cannot be null");
        validateLead(payment.getLead());

        createEvent(
                TrackingEventType.PAYMENT_APPROVED,
                payment.getLead(),
                payment.getId(),
                "stripeSessionId=" + payment.getStripeSessionId()
        );
    }

    private void createEvent(TrackingEventType type,
                             Lead lead,
                             Long paymentId,
                             String metadata) {

        TrackingEvent event = TrackingEvent.builder()
                .eventType(type)
                .lead(lead)
                .paymentId(paymentId)
                .metadata(metadata)
                .createdAt(OffsetDateTime.now())
                .build();

        trackingEventRepository.save(event);
    }

    private void validateLead(Lead lead) {
        Objects.requireNonNull(lead, "Lead cannot be null");
    }
}