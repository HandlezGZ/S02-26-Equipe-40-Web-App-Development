package com.nocountry.conversionflow.conversionflow_api.controller.dto;

public record AttributionEnrichmentRequest(
        Long leadId,
        String externalId,
        String gclid,
        String fbclid,
        String fbp,
        String fbc
) {
}
