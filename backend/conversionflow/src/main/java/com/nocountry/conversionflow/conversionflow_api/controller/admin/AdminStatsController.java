package com.nocountry.conversionflow.conversionflow_api.controller.admin;

import com.nocountry.conversionflow.conversionflow_api.controller.admin.dto.AdminOverviewResponse;
import com.nocountry.conversionflow.conversionflow_api.controller.admin.dto.AdminTimeSeriesResponse;
import com.nocountry.conversionflow.conversionflow_api.controller.admin.dto.DispatchFailureResponse;
import com.nocountry.conversionflow.conversionflow_api.service.stats.AdminStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview(
            @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        return adminStatsService.getOverview(days);
    }

    @GetMapping("/timeseries")
    public AdminTimeSeriesResponse timeSeries(
            @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        return adminStatsService.getTimeSeries(days);
    }

    @GetMapping("/dispatch/failures")
    public List<DispatchFailureResponse> recentDispatchFailures(
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        return adminStatsService.getRecentDispatchFailures(limit);
    }
}
