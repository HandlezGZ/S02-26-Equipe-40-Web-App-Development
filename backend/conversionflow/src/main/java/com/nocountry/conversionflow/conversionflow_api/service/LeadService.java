package com.nocountry.conversionflow.conversionflow_api.service;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.LeadStatus;
import com.nocountry.conversionflow.conversionflow_api.repository.LeadRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class LeadService {

    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public Lead createLead(String externalId, String email) {

        if (leadRepository.existsByExternalId(externalId)) {
            throw new RuntimeException("Lead already exists with this externalId");
        }

        Lead lead = new Lead();
        lead.setExternalId(externalId);
        lead.setEmail(email);
        lead.setStatus(LeadStatus.NEW);
        lead.setCreatedAt(OffsetDateTime.now());

        return leadRepository.save(lead);
    }
}
