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

        Map<String, Object> userData = new HashMap<>();

        if (event.getEmail() != null) {
            userData.put("em", hash(event.getEmail().trim().toLowerCase()));
        }

        Map<String, Object> customData = new HashMap<>();
        customData.put("currency", "USD");
        customData.put("value", event.getConvertedAmount());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event_name", "Purchase");
        eventData.put("event_time", Instant.now().getEpochSecond());
        eventData.put("action_source", "website");
        eventData.put("user_data", userData);
        eventData.put("custom_data", customData);

        Map<String, Object> payload = new HashMap<>();
        payload.put("data", Collections.singletonList(eventData));

        return payload;
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