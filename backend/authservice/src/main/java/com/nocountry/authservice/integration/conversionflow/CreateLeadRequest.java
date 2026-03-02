package com.nocountry.authservice.integration.conversionflow;

public record CreateLeadRequest(
        String externalId,
        String email,
        String gclid,
        String fbclid,
        String fbp,
        String fbc,
        String utmSource,
        String utmCampaign
) {
}
