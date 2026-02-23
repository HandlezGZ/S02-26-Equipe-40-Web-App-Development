package com.nocountry.conversionflow.conversionflow_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "meta")
public class MetaProperties {

    private String pixelId;
    private String accessToken;

    public String getPixelId() {
        return pixelId;
    }

    public void setPixelId(String pixelId) {
        this.pixelId = pixelId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}