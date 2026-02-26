package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CapturePixelEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(CapturePixelEventUseCase.class);

    private final LeadRepository leadRepository;

    public CapturePixelEventUseCase(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Transactional
    public void execute(Long leadId, String externalId, String gclid, String fbclid, String fbp, String fbc) {
        if (leadId == null && (externalId == null || externalId.isBlank())) {
            throw new IllegalArgumentException("leadId or externalId is required");
        }

        Lead lead = resolveLead(leadId, externalId);
        lead.updateTracking(gclid, fbclid, fbp, fbc);
        leadRepository.save(lead);

        log.info("usecase.pixelEvent.capture.success leadId={} externalId={} gclid={} fbclid={} fbp={} fbc={}",
                lead.getId(),
                lead.getExternalId(),
                blankToNull(gclid) != null,
                blankToNull(fbclid) != null,
                blankToNull(fbp) != null,
                blankToNull(fbc) != null);
    }

    private Lead resolveLead(Long leadId, String externalId) {
        if (leadId != null) {
            return leadRepository.findById(leadId)
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));
        }

        return leadRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found for externalId: " + externalId));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
