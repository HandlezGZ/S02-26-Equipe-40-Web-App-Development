package com.nocountry.conversionflow.conversionflow_api.domain.repository.projection;

import java.time.LocalDate;

public interface DailyLeadCountProjection {
    LocalDate getDay();
    Long getLeads();
}
