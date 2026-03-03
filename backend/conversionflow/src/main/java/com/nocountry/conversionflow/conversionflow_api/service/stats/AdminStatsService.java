package com.nocountry.conversionflow.conversionflow_api.service.stats;

import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.controller.admin.dto.AdminOverviewResponse;
import com.nocountry.conversionflow.conversionflow_api.controller.admin.dto.AdminTimeSeriesResponse;
import com.nocountry.conversionflow.conversionflow_api.controller.admin.dto.DispatchFailureResponse;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.ConversionDispatch;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.DispatchStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.LeadStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.PaymentStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.ConversionDispatchRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.PaymentRepository;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.projection.DailyLeadCountProjection;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.projection.DailyPaymentStatsProjection;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.projection.ProviderDispatchStatusCountProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatsService {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;
    private static final int DEFAULT_DAYS = 30;

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_LIMIT = 20;

    private final LeadRepository leadRepository;
    private final PaymentRepository paymentRepository;
    private final ConversionDispatchRepository dispatchRepository;
    private final StripeProperties stripeProperties;

    public AdminStatsService(
            LeadRepository leadRepository,
            PaymentRepository paymentRepository,
            ConversionDispatchRepository dispatchRepository,
            StripeProperties stripeProperties
    ) {
        this.leadRepository = leadRepository;
        this.paymentRepository = paymentRepository;
        this.dispatchRepository = dispatchRepository;
        this.stripeProperties = stripeProperties;
    }

    public AdminOverviewResponse getOverview(Integer days) {
        int windowDays = normalizeDays(days);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = now.minusDays(windowDays);

        long totalLeads = leadRepository.count();
        long leadsInWindow = leadRepository.countCreatedAtOrAfter(from);
        long wonLeads = leadRepository.countByStatus(LeadStatus.WON);
        long wonInWindow = leadRepository.countConvertedAtOrAfter(from);

        Map<String, Long> leadByStatus = new LinkedHashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            leadByStatus.put(status.name(), leadRepository.countByStatus(status));
        }

        long paidTotal = paymentRepository.countByStatus(PaymentStatus.PAID);
        long paidInWindow = paymentRepository.countByStatusAndCreatedAtOrAfter(PaymentStatus.PAID, from);

        long totalRevenueCents = safeLong(paymentRepository.sumAmountCentsByStatus(PaymentStatus.PAID));
        long windowRevenueCents = safeLong(paymentRepository.sumAmountCentsByStatusAndCreatedAtOrAfter(PaymentStatus.PAID, from));

        long dispatchPending = dispatchRepository.countByStatus(DispatchStatus.PENDING);
        long dispatchSuccess = dispatchRepository.countByStatus(DispatchStatus.SUCCESS);
        long dispatchFailed = dispatchRepository.countByStatus(DispatchStatus.FAILED);
        long dispatchTotal = dispatchPending + dispatchSuccess + dispatchFailed;

        double avgAttempts = safeDouble(dispatchRepository.findAverageAttemptCount());

        List<AdminOverviewResponse.ProviderDispatchStats> byProvider = buildProviderDispatchStats();

        AdminOverviewResponse.LeadOverview leadOverview = new AdminOverviewResponse.LeadOverview(
                totalLeads,
                leadsInWindow,
                wonLeads,
                wonInWindow,
                percent(wonLeads, totalLeads),
                leadByStatus
        );

        AdminOverviewResponse.PaymentOverview paymentOverview =
                new AdminOverviewResponse.PaymentOverview(paidTotal, paidInWindow);

        AdminOverviewResponse.RevenueOverview revenueOverview =
                new AdminOverviewResponse.RevenueOverview(
                        totalRevenueCents,
                        windowRevenueCents,
                        centsToBigDecimal(totalRevenueCents),
                        centsToBigDecimal(windowRevenueCents),
                        safeCurrency(stripeProperties.getCurrency())
                );

        AdminOverviewResponse.DispatchOverview dispatchOverview =
                new AdminOverviewResponse.DispatchOverview(
                        dispatchTotal,
                        dispatchPending,
                        dispatchSuccess,
                        dispatchFailed,
                        percent(dispatchFailed, dispatchTotal),
                        round(avgAttempts),
                        byProvider
                );

        AdminOverviewResponse.AttributionOverview attributionOverview =
                new AdminOverviewResponse.AttributionOverview(
                        leadRepository.countWithGclid(),
                        leadRepository.countWithFbclid(),
                        leadRepository.countWithFbp(),
                        leadRepository.countWithFbc()
                );

        return new AdminOverviewResponse(
                windowDays,
                now,
                leadOverview,
                paymentOverview,
                revenueOverview,
                dispatchOverview,
                attributionOverview
        );
    }

    public AdminTimeSeriesResponse getTimeSeries(Integer days) {
        int windowDays = normalizeDays(days);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = now.minusDays(windowDays);

        Map<LocalDate, AdminTimeSeriesResponse.DailyStatsPoint> pointsByDay = new HashMap<>();

        for (DailyLeadCountProjection row : leadRepository.findDailyLeadCounts(from)) {
            pointsByDay.put(row.getDay(), new AdminTimeSeriesResponse.DailyStatsPoint(
                    row.getDay(),
                    safeLong(row.getLeads()),
                    0,
                    0
            ));
        }

        for (DailyPaymentStatsProjection row : paymentRepository.findDailyPaidPaymentStats(from)) {
            AdminTimeSeriesResponse.DailyStatsPoint existing = pointsByDay.get(row.getDay());
            if (existing == null) {
                pointsByDay.put(row.getDay(), new AdminTimeSeriesResponse.DailyStatsPoint(
                        row.getDay(),
                        0,
                        safeLong(row.getPaidPayments()),
                        safeLong(row.getRevenueCents())
                ));
            } else {
                pointsByDay.put(row.getDay(), new AdminTimeSeriesResponse.DailyStatsPoint(
                        row.getDay(),
                        existing.leads(),
                        safeLong(row.getPaidPayments()),
                        safeLong(row.getRevenueCents())
                ));
            }
        }

        List<AdminTimeSeriesResponse.DailyStatsPoint> points = new ArrayList<>();
        LocalDate cursor = from.toLocalDate();
        LocalDate end = now.toLocalDate();
        while (!cursor.isAfter(end)) {
            points.add(pointsByDay.getOrDefault(
                    cursor,
                    new AdminTimeSeriesResponse.DailyStatsPoint(cursor, 0, 0, 0)
            ));
            cursor = cursor.plusDays(1);
        }

        return new AdminTimeSeriesResponse(
                windowDays,
                from.toLocalDate(),
                now.toLocalDate(),
                points
        );
    }

    public List<DispatchFailureResponse> getRecentDispatchFailures(Integer limit) {
        int normalizedLimit = normalizeLimit(limit);

        List<ConversionDispatch> failures = dispatchRepository.findByStatusOrderByLastAttemptAtDesc(
                DispatchStatus.FAILED,
                PageRequest.of(0, normalizedLimit)
        );

        return failures.stream()
                .map(item -> new DispatchFailureResponse(
                        item.getId(),
                        item.getLeadId(),
                        item.getProvider(),
                        item.getAttemptCount(),
                        item.getErrorMessage(),
                        item.getLastAttemptAt(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    private List<AdminOverviewResponse.ProviderDispatchStats> buildProviderDispatchStats() {
        Map<Provider, ProviderBucket> providerBuckets = new EnumMap<>(Provider.class);
        for (Provider provider : Provider.values()) {
            providerBuckets.put(provider, new ProviderBucket());
        }

        for (ProviderDispatchStatusCountProjection row : dispatchRepository.findProviderDispatchStatusCounts()) {
            Provider provider = Provider.valueOf(row.getProvider());
            DispatchStatus status = DispatchStatus.valueOf(row.getStatus());
            long total = safeLong(row.getTotal());

            ProviderBucket bucket = providerBuckets.get(provider);
            switch (status) {
                case PENDING -> bucket.pending += total;
                case SUCCESS -> bucket.success += total;
                case FAILED -> bucket.failed += total;
            }
        }

        return providerBuckets.entrySet().stream()
                .map(entry -> new AdminOverviewResponse.ProviderDispatchStats(
                        entry.getKey().name(),
                        entry.getValue().pending,
                        entry.getValue().success,
                        entry.getValue().failed,
                        entry.getValue().pending + entry.getValue().success + entry.getValue().failed
                ))
                .toList();
    }

    private int normalizeDays(Integer days) {
        if (days == null) return DEFAULT_DAYS;
        return Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        return Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, limit));
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double percent(long numerator, long denominator) {
        if (denominator == 0) return 0.0;
        return round((numerator * 100.0) / denominator);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal centsToBigDecimal(long cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String safeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase();
    }

    private static class ProviderBucket {
        private long pending;
        private long success;
        private long failed;
    }
}
