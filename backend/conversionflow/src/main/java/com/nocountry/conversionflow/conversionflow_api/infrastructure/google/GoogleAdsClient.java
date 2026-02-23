package com.nocountry.conversionflow.conversionflow_api.infrastructure.google;

import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;

public interface GoogleAdsClient {

    void sendConversion(LeadConvertedEvent event);

}