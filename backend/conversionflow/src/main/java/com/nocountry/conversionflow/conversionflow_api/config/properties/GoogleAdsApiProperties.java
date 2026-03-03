package com.nocountry.conversionflow.conversionflow_api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Config para envio de conversões via Google Ads API (UploadClickConversions).
 */
@Configuration
@ConfigurationProperties(prefix = "google.ads.api")
public class GoogleAdsApiProperties {

    /** Ex: "v23". */
    private String apiVersion = "v23";
    private String developerToken;
    private String customerId;
    private String loginCustomerId;
    /** Ex: customers/1234567890/conversionActions/987654321 */
    private String conversionActionResourceName;

    // OAuth2 (recomendado)
    private String oauthClientId;
    private String oauthClientSecret;
    private String oauthRefreshToken;

    /** Opcional para MVP: token manual. */
    private String accessToken;

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public String getDeveloperToken() { return developerToken; }
    public void setDeveloperToken(String developerToken) { this.developerToken = developerToken; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getLoginCustomerId() { return loginCustomerId; }
    public void setLoginCustomerId(String loginCustomerId) { this.loginCustomerId = loginCustomerId; }

    public String getConversionActionResourceName() { return conversionActionResourceName; }
    public void setConversionActionResourceName(String conversionActionResourceName) { this.conversionActionResourceName = conversionActionResourceName; }

    public String getOauthClientId() { return oauthClientId; }
    public void setOauthClientId(String oauthClientId) { this.oauthClientId = oauthClientId; }

    public String getOauthClientSecret() { return oauthClientSecret; }
    public void setOauthClientSecret(String oauthClientSecret) { this.oauthClientSecret = oauthClientSecret; }

    public String getOauthRefreshToken() { return oauthRefreshToken; }
    public void setOauthRefreshToken(String oauthRefreshToken) { this.oauthRefreshToken = oauthRefreshToken; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
