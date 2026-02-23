package com.nocountry.conversionflow.conversionflow_api.service.pipedrive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.pipedrive.PipedriveClient;
import org.springframework.stereotype.Service;

@Service
public class PipedriveService {

    private final PipedriveClient pipedriveClient;
    private final ObjectMapper objectMapper;

    public PipedriveService(PipedriveClient pipedriveClient, ObjectMapper objectMapper) {
        this.pipedriveClient = pipedriveClient;
        this.objectMapper = objectMapper;
    }

    public void syncFromPayload(String payload) {
        try {
            LeadConvertedEvent event = objectMapper.readValue(payload, LeadConvertedEvent.class);
            syncConvertedLead(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse payload for Pipedrive sync", e);
        }
    }

    public void syncConvertedLead(LeadConvertedEvent event) {
        pipedriveClient.syncConvertedLead(event);
    }
}