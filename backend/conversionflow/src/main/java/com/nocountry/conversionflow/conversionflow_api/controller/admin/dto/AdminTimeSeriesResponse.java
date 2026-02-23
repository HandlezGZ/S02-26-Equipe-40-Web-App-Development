package com.nocountry.conversionflow.conversionflow_api.controller.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminTimeSeriesResponse(
        int windowDays,
        LocalDate fromDate,
        LocalDate toDate,
        List<DailyStatsPoint> points
) {

    public record DailyStatsPoint(
            LocalDate day,
            long leads,
            long paidPayments,
            long revenueCents
    ) {
    }
}
