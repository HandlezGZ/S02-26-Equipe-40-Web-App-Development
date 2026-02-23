package com.nocountry.conversionflow.conversionflow_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private String secretKey;
    private String webhookSecret;

    private String successUrl;
    private String cancelUrl;

    /**
     * Ex: "usd"
     */
    private String currency = "usd";

    /**
     * stripe.prices.basic=price_...
     * stripe.prices.pro=price_...
     */
    private Map<String, String> prices;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, String> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, String> prices) {
        this.prices = prices;
    }
}