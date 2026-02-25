package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.service.stripe.StripeWebhookService;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StartCheckoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartCheckoutUseCase.class);

    private final StripeWebhookService stripeWebhookService;

    public StartCheckoutUseCase(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    public String execute(Long leadId, String plan, String gclid, String fbclid, String fbp, String fbc)
            throws StripeException {
        log.info("usecase.startCheckout.start leadId={} plan={}", leadId, plan);
        String checkoutUrl = stripeWebhookService.createCheckoutSession(leadId, plan, gclid, fbclid, fbp, fbc);
        log.info("usecase.startCheckout.success leadId={} plan={}", leadId, plan);
        return checkoutUrl;
    }
}
