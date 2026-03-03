package com.nocountry.conversionflow.conversionflow_api.domain.repository.projection;

public interface ProviderDispatchStatusCountProjection {
    String getProvider();
    String getStatus();
    Long getTotal();
}
