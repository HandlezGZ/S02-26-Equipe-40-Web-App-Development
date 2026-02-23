package com.nocountry.conversionflow.conversionflow_api.infrastructure.meta;

import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;

public interface MetaApiClient {

    void sendConversion(LeadConvertedEvent event);

}