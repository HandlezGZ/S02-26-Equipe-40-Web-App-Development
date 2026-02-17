package com.nocountry.conversionflow.conversionflow_api.service;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.repository.LeadRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StripeService {

    private final StripeProperties stripeProperties;
    private final LeadRepository leadRepository;

    @Value("${app.checkout.success-url}")
    private String successUrl;

    @Value("${app.checkout.cancel-url}")
    private String cancelUrl;

    public StripeService(StripeProperties stripeProperties,
                         LeadRepository leadRepository) {
        this.stripeProperties = stripeProperties;
        this.leadRepository = leadRepository;
    }

    public String createCheckoutSession(Long leadId, String planKey) {

        Stripe.apiKey = stripeProperties.getSecretKey();

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        // ⚠️ Aqui você precisa mapear o priceId corretamente
        String priceId = System.getenv("STRIPE_PRICE_" + planKey);

        if (priceId == null) {
            throw new RuntimeException("PriceId not configured for plan: " + planKey);
        }

        try {

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl(successUrl)
                            .setCancelUrl(cancelUrl)
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPrice(priceId)
                                            .build()
                            )
                            .putAllMetadata(Map.of(
                                    "leadId", lead.getId().toString(),
                                    "plan", planKey
                            ))
                            .build();

            Session session = Session.create(params);

            return session.getUrl();

        } catch (Exception e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }
}
