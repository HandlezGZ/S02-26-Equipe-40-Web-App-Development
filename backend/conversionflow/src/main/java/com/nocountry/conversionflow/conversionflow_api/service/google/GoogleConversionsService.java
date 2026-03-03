package com.nocountry.conversionflow.conversionflow_api.service.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.google.GoogleAdsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoogleConversionsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleConversionsService.class);

    private final ObjectMapper objectMapper;
    private final GoogleAdsClient googleAdsClient;

    public GoogleConversionsService(ObjectMapper objectMapper, GoogleAdsClient googleAdsClient) {
        this.objectMapper = objectMapper;
        this.googleAdsClient = googleAdsClient;
    }

    public void sendConversionFromPayload(String payload) {
        try {
            LeadConvertedEvent event = objectMapper.readValue(payload, LeadConvertedEvent.class);
            sendConversion(event);
        } catch (Exception e) {
            log.error("google.payload.parse.error payloadSize={}", payload == null ? 0 : payload.length(), e);
            throw new RuntimeException("Failed to parse payload for Google conversion", e);
        }
    }

    /**
     * Envia conversão para o Google Ads API (UploadClickConversions).
     */
    public void sendConversion(LeadConvertedEvent event) {

        if (event.getGclid() == null || event.getGclid().isBlank()) {
            log.info("Google conversion skipped. No gclid. leadId={}", event.getLeadId());
            return;
        }

        googleAdsClient.sendConversion(event);
        log.info("Google conversion sent. leadId={}, gclid={}, value={}, currency={}, timestamp={}",
                event.getLeadId(),
                event.getGclid(),
                event.getConvertedAmount(),
                event.getCurrency(),
                event.getConvertedAt());
    }
}
