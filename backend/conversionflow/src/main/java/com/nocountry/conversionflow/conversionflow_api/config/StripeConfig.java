package com.nocountry.conversionflow.conversionflow_api.config;

import com.stripe.Stripe;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

    /**
     * Ao subir a aplicação, configuramos a API Key do Stripe SDK.
     * Isso evita ficar setando em todo método.
     */
    public StripeConfig(StripeProperties properties) {
        Stripe.apiKey = properties.getSecretKey();
    }
}
