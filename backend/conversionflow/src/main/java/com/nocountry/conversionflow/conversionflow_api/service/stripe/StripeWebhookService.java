package com.nocountry.conversionflow.conversionflow_api.service.stripe;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeProperties stripeProperties;
    private final LeadRepository leadRepository;

    public StripeWebhookService(
            StripeProperties stripeProperties,
            LeadRepository leadRepository
    ) {
        this.stripeProperties = stripeProperties;
        this.leadRepository = leadRepository;
    }

    @Transactional
    public String createCheckoutSession(
            Long leadId,
            String plan,
            String gclid,
            String fbclid,
            String fbp,
            String fbc,
            String utmSource,
            String utmCampaign
    ) throws StripeException {
        log.info("stripe.checkout.create start leadId={} plan={}", leadId, plan);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        lead.updateTracking(gclid, fbclid, fbp, fbc, utmSource, utmCampaign);
        lead.startCheckout();
        leadRepository.save(lead);

        String normalizedPlan = plan.trim().toLowerCase(Locale.ROOT);
        String priceId = resolvePriceId(normalizedPlan);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeProperties.getSuccessUrl())
                .setCancelUrl(stripeProperties.getCancelUrl())
                .putMetadata("leadId", String.valueOf(lead.getId()))
                .putMetadata("plan", normalizedPlan)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                )
                .build();

        Session session = Session.create(params);

        if (session.getUrl() == null || session.getUrl().isBlank()) {
            throw new IllegalStateException("Stripe did not return checkout URL");
        }

        log.info("stripe.checkout.create success leadId={} plan={} sessionId={}",
                leadId, normalizedPlan, session.getId());
        return session.getUrl();
    }

    private String resolvePriceId(String normalizedPlan) {
        Map<String, String> prices = stripeProperties.getPrices();
        if (prices == null || prices.isEmpty()) {
            throw new IllegalStateException("stripe.prices is not configured");
        }

        String priceId = prices.get(normalizedPlan);
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalArgumentException("No Stripe priceId configured for plan: " + normalizedPlan);
        }

        return priceId;
    }
}
