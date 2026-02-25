package com.nocountry.conversionflow.conversionflow_api.domain.repository.projection;

import java.time.LocalDate;

public interface DailyPaymentStatsProjection {
    LocalDate getDay();
    Long getPaidPayments();
    Long getRevenueCents();
}
