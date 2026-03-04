package com.nocountry.authservice.integration.conversionflow;

public record ConversionFlowClientDiagnostics(
        boolean circuitOpen,
        int consecutiveFailures,
        long successCount,
        long failureCount,
        long retryCount,
        long circuitOpenCount
) {
}
