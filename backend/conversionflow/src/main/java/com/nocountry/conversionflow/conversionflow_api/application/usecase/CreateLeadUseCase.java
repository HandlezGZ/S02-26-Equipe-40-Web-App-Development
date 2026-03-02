package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.application.exception.DuplicateLeadException;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateLeadUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateLeadUseCase.class);

    private final LeadRepository leadRepository;

    public CreateLeadUseCase(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public Lead execute(
            String externalId,
            String email,
            String gclid,
            String fbclid,
            String fbp,
            String fbc,
            String utmSource,
            String utmCampaign
    ) {
        log.info("usecase.createLead.start externalId={} email={}", externalId, email);
        if (leadRepository.existsByExternalId(externalId)) {
            log.warn("usecase.createLead.duplicate externalId={}", externalId);
            throw new DuplicateLeadException("Lead already exists with this externalId");
        }

        Lead lead = new Lead(externalId, email);
        lead.updateTracking(gclid, fbclid, fbp, fbc, utmSource, utmCampaign);

        Lead savedLead = leadRepository.save(lead);
        log.info("usecase.createLead.success leadId={} externalId={}", savedLead.getId(), savedLead.getExternalId());
        return savedLead;
    }
}
