package com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class StripeSdkClient implements StripeClient {

    private final StripeProperties stripeProperties;

    public StripeSdkClient(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
        Stripe.apiKey = stripeProperties.getSecretKey();
    }

    @Override
    public Session createCheckoutSession(
            String currency,
            BigDecimal amount,
            String successUrl,
            String cancelUrl,
            Map<String, String> metadata
    ) {

        try {

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl(successUrl)
                            .setCancelUrl(cancelUrl)
                            .putAllMetadata(metadata)
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency(currency)
                                                            .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                            .setName("ConversionFlow Product")
                                                                            .build()
                                                            )
                                                            .build()
                                            )
                                            .build()
                            )
                            .build();

            return Session.create(params);

        } catch (StripeException e) {
            throw new RuntimeException("Error creating Stripe checkout session", e);
        }
    }

    @Override
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new RuntimeException("Error retrieving Stripe payment intent", e);
        }
    }
}