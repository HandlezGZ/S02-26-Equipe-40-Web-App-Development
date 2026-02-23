package com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe;

import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import java.math.BigDecimal;
import java.util.Map;

public interface StripeClient {

    Session createCheckoutSession(
            String currency,
            BigDecimal amount,
            String successUrl,
            String cancelUrl,
            Map<String, String> metadata
    );

    PaymentIntent retrievePaymentIntent(String paymentIntentId);
}