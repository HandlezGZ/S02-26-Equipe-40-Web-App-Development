package com.nocountry.conversionflow.conversionflow_api.controller.dto;

public record CreateLeadRequest(
        String externalId,
        String email
) {}
