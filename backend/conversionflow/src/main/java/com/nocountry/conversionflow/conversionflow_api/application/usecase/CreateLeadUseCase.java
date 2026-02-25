package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.service.LeadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateLeadUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateLeadUseCase.class);

    private final LeadService leadService;

    public CreateLeadUseCase(LeadService leadService) {
        this.leadService = leadService;
    }

    public Lead execute(String externalId, String email, String gclid, String fbclid, String fbp, String fbc) {
        log.info("usecase.createLead.start externalId={} email={}", externalId, email);
        Lead lead = leadService.createLead(externalId, email, gclid, fbclid, fbp, fbc);
        log.info("usecase.createLead.success leadId={} externalId={}", lead.getId(), lead.getExternalId());
        return lead;
    }
}
