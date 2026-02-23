package com.nocountry.conversionflow.conversionflow_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.ads")
public class GoogleAdsProperties {

    private String developerToken;
    private String customerId;
    private String conversionActionId;
    private String accessToken;

    public String getDeveloperToken() {
        return developerToken;
    }

    public void setDeveloperToken(String developerToken) {
        this.developerToken = developerToken;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getConversionActionId() {
        return conversionActionId;
    }

    public void setConversionActionId(String conversionActionId) {
        this.conversionActionId = conversionActionId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}