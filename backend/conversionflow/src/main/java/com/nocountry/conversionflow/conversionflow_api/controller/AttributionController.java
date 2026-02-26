package com.nocountry.conversionflow.conversionflow_api.controller;

import com.nocountry.conversionflow.conversionflow_api.application.usecase.EnrichAttributionUseCase;
import com.nocountry.conversionflow.conversionflow_api.controller.dto.AttributionEnrichmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attribution")
public class AttributionController {

    private static final Logger log = LoggerFactory.getLogger(AttributionController.class);

    private final EnrichAttributionUseCase enrichAttributionUseCase;

    public AttributionController(EnrichAttributionUseCase enrichAttributionUseCase) {
        this.enrichAttributionUseCase = enrichAttributionUseCase;
    }

    @PostMapping("/enrichment")
    public ResponseEntity<Void> enrich(@RequestBody AttributionEnrichmentRequest request) {
        log.info("attribution.enrichment.request leadId={} externalId={}", request.leadId(), request.externalId());

        enrichAttributionUseCase.execute(
                request.leadId(),
                request.externalId(),
                request.gclid(),
                request.fbclid(),
                request.fbp(),
                request.fbc()
        );

        return ResponseEntity.accepted().build();
    }
}
