package com.nocountry.conversionflow.conversionflow_api.infrastructure.pipedrive;

import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;

public interface PipedriveClient {

    void syncConvertedLead(LeadConvertedEvent event);

}