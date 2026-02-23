package com.nocountry.conversionflow.conversionflow_api.controller.admin.dto;

import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;

import java.time.LocalDateTime;

public record DispatchFailureResponse(
        Long id,
        Long leadId,
        Provider provider,
        int attemptCount,
        String errorMessage,
        LocalDateTime lastAttemptAt,
        LocalDateTime createdAt
) {
}
