package com.nocountry.conversionflow.conversionflow_api.controller.admin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record AdminOverviewResponse(
        int windowDays,
        OffsetDateTime generatedAt,
        LeadOverview leads,
        PaymentOverview payments,
        RevenueOverview revenue,
        DispatchOverview dispatches,
        AttributionOverview attribution
) {

    public record LeadOverview(
            long total,
            long createdInWindow,
            long won,
            long wonInWindow,
            double leadToWonRatePercent,
            Map<String, Long> byStatus
    ) {
    }

    public record PaymentOverview(
            long paidTotal,
            long paidInWindow
    ) {
    }

    public record RevenueOverview(
            long totalCents,
            long windowCents,
            BigDecimal total,
            BigDecimal inWindow,
            String currency
    ) {
    }

    public record DispatchOverview(
            long total,
            long pending,
            long success,
            long failed,
            double failureRatePercent,
            double averageAttempts,
            List<ProviderDispatchStats> byProvider
    ) {
    }

    public record ProviderDispatchStats(
            String provider,
            long pending,
            long success,
            long failed,
            long total
    ) {
    }

    public record AttributionOverview(
            long withGclid,
            long withFbclid,
            long withFbp,
            long withFbc
    ) {
    }
}
