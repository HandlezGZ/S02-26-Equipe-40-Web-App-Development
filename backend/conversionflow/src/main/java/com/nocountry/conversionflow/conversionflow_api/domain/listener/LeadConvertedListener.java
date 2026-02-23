package com.nocountry.conversionflow.conversionflow_api.domain.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.ConversionDispatch;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.ConversionDispatchRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class LeadConvertedListener {

    private final ConversionDispatchRepository repository;
    private final ObjectMapper objectMapper;

    public LeadConvertedListener(ConversionDispatchRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLeadConverted(LeadConvertedEvent event) {

        String payload = serializeEvent(event);

        createDispatch(event.getLeadId(), Provider.GOOGLE, payload);
        createDispatch(event.getLeadId(), Provider.META, payload);
        createDispatch(event.getLeadId(), Provider.PIPEDRIVE, payload);
    }

    private void createDispatch(Long leadId, Provider provider, String payload) {
        ConversionDispatch dispatch = new ConversionDispatch(leadId, provider, payload);
        repository.save(dispatch);
    }

    private String serializeEvent(LeadConvertedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize LeadConvertedEvent", e);
        }
    }
}