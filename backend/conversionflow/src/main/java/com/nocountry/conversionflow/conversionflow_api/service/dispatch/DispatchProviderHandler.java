package com.nocountry.conversionflow.conversionflow_api.service.dispatch;

import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;

public interface DispatchProviderHandler {

    Provider provider();

    void dispatch(String payload);
}
