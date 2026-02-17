package com.nocountry.conversionflow.conversionflow_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Carrega configs do Stripe vindas do application.yml,
 * que por sua vez vem de variáveis de ambiente.
 *
 * Vantagem: tipado, organizado e sem hardcode.
 */
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    /**
     * STRIPE_SECRET_KEY (sk_test_... / sk_live_...)
     */
    private String secretKey;

    /**
     * STRIPE_WEBHOOK_SECRET (whsec_...)
     */
    private String webhookSecret;

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
