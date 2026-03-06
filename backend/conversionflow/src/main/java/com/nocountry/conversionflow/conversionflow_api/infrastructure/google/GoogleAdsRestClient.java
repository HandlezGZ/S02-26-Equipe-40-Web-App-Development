package com.nocountry.conversionflow.conversionflow_api.infrastructure.google;

import com.nocountry.conversionflow.conversionflow_api.config.properties.GoogleAdsApiProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.event.LeadConvertedEvent;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementação REST do Google Ads ConversionUploadService.UploadClickConversions.
 *
 * Requisitos:
 * - gclid
 * - conversion action resource name
 * - conversion date time
 * - developer token
 * - OAuth2 access token
 */
@Component
public class GoogleAdsRestClient implements GoogleAdsClient {

    private static final DateTimeFormatter GOOGLE_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxxx");

    private final GoogleAdsApiProperties properties;
    private final GoogleAdsOAuthService oauthService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleAdsRestClient(GoogleAdsApiProperties properties, GoogleAdsOAuthService oauthService) {
        this.properties = properties;
        this.oauthService = oauthService;
    }

    @Override
    public void sendConversion(LeadConvertedEvent event) {

        if (event.getGclid() == null || event.getGclid().isBlank()) {
            return; // sem gclid não tem como importar click conversion
        }

        if (isBlank(properties.getDeveloperToken())) {
            throw new IllegalStateException("Google Ads API developer token não configurado (google.ads.api.developer-token)");
        }
        if (isBlank(properties.getCustomerId())) {
            throw new IllegalStateException("Google Ads API customerId não configurado (google.ads.api.customer-id)");
        }
        if (isBlank(properties.getConversionActionResourceName())) {
            throw new IllegalStateException("Google Ads conversion action resource name não configurado (google.ads.api.conversion-action-resource-name)");
        }

        String accessToken = oauthService.getAccessToken();

        String url = String.format(
                "https://googleads.googleapis.com/%s/customers/%s:uploadClickConversions",
                normalizeApiVersion(properties.getApiVersion()),
                properties.getCustomerId().replaceAll("-", "")
        );

        Map<String, Object> conversion = new LinkedHashMap<>();
        conversion.put("gclid", event.getGclid());
        conversion.put("conversionAction", properties.getConversionActionResourceName());
        conversion.put("conversionDateTime", formatConversionTime(event.getConvertedAt()));

        BigDecimal value = event.getConvertedAmount();
        if (value != null) {
            conversion.put("conversionValue", value);
        }

        conversion.put("currencyCode", normalizeCurrency(event.getCurrency()));

        // Dedupe/auditoria (opcional)
        if (event.getPaymentIntentId() != null && !event.getPaymentIntentId().isBlank()) {
            conversion.put("orderId", event.getPaymentIntentId());
        } else if (event.getExternalId() != null && !event.getExternalId().isBlank()) {
            conversion.put("orderId", event.getExternalId());
        }

        Map<String, Object> body = Map.of(
                "partialFailure", true,
                "validateOnly", false,
                "conversions", List.of(conversion)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("developer-token", properties.getDeveloperToken());
        if (!isBlank(properties.getLoginCustomerId())) {
            headers.set("login-customer-id", properties.getLoginCustomerId().replaceAll("-", ""));
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error sending conversion to Google Ads API: status=" + response.getStatusCode() + " body=" + response.getBody());
        }
    }

    private static String normalizeApiVersion(String v) {
        if (v == null || v.isBlank()) return "v23";
        return v.trim().startsWith("v") ? v.trim() : ("v" + v.trim());
    }

    private static String formatConversionTime(OffsetDateTime odt) {
        if (odt == null) {
            odt = OffsetDateTime.now();
        }
        return odt.format(GOOGLE_DT);
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return "BRL";
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
