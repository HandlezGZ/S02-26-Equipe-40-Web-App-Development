package com.nocountry.conversionflow.conversionflow_api.service;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import org.springframework.stereotype.Service;

@Service
public class LeadService {

    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public Lead createLead(String externalId, String email, String gclid, String fbclid, String fbp, String fbc) {

        if (leadRepository.existsByExternalId(externalId)) {
            throw new RuntimeException("Lead already exists with this externalId");
        }

        Lead lead = new Lead(externalId, email);
        lead.updateTracking(gclid, fbclid, fbp, fbc);

        return leadRepository.save(lead);
    }
}