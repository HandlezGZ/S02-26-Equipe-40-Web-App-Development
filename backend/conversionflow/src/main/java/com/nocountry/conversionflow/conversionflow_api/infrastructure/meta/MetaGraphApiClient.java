package com.nocountry.conversionflow.conversionflow_api.infrastructure.meta;

import com.nocountry.conversionflow.conversionflow_api.config.properties.MetaProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Cliente simples (MVP) para Meta Conversions API (CAPI).
 *
 * Endpoint:
 *   POST https://graph.facebook.com/v18.0/{PIXEL_ID}/events?access_token=...
 *
 * Campos importantes:
 * - event_id para dedupe (ideal: paymentIntentId)
 * - user_data: em (hash sha256), fbp, fbc
 * - custom_data: value, currency
 */
@Component
public class MetaGraphApiClient implements MetaApiClient {

    private final MetaProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public MetaGraphApiClient(MetaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendConversion(LeadConvertedEvent event) {

        String url = String.format(
                "https://graph.facebook.com/v18.0/%s/events",
                properties.getPixelId()
        );

        Map<String, Object> payload = buildPayload(event);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(payload, headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url + "?access_token=" + properties.getAccessToken(),
                        HttpMethod.POST,
                        request,
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error sending conversion to Meta: " + response.getBody());
        }
    }

    private Map<String, Object> buildPayload(LeadConvertedEvent event) {

        Map<String, Object> userData = new LinkedHashMap<>();

        // email hash (sha256 lower(trim(email)))
        if (event.getEmail() != null && !event.getEmail().isBlank()) {
            userData.put("em", List.of(hash(event.getEmail().trim().toLowerCase(Locale.ROOT))));
        }

        // fbp / fbc aumentam bastante match rate (não são hash)
        if (event.getFbp() != null && !event.getFbp().isBlank()) {
            userData.put("fbp", event.getFbp());
        }
        if (event.getFbc() != null && !event.getFbc().isBlank()) {
            userData.put("fbc", event.getFbc());
        }

        // custom_data
        Map<String, Object> customData = new LinkedHashMap<>();
        customData.put("value", event.getConvertedAmount());
        customData.put("currency", normalizeCurrency(event.getCurrency()));

        // event_time: use convertedAt quando disponível (melhor para auditoria)
        long eventTime = Instant.now().getEpochSecond();
        if (event.getConvertedAt() != null) {
            eventTime = event.getConvertedAt().toInstant().getEpochSecond();
        }

        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("event_name", "Purchase");
        eventData.put("event_time", eventTime);
        eventData.put("action_source", "website");

        // event_id para dedupe Pixel + CAPI (ideal: payment_intent id)
        if (event.getPaymentIntentId() != null && !event.getPaymentIntentId().isBlank()) {
            eventData.put("event_id", event.getPaymentIntentId());
        }

        eventData.put("user_data", userData);
        eventData.put("custom_data", customData);

        Map<String, Object> payload = new HashMap<>();
        payload.put("data", Collections.singletonList(eventData));

        return payload;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return "BRL";
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing value for Meta", e);
        }
    }
}
