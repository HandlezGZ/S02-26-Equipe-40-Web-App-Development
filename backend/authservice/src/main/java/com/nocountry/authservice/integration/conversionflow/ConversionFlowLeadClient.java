package com.nocountry.authservice.integration.conversionflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ConversionFlowLeadClient {

    private final RestClient restClient;
    private final String apiKey;

    public ConversionFlowLeadClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.conversionflow.base-url}") String baseUrl,
            @Value("${app.conversionflow.api-key:}") String apiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public void createLead(CreateLeadRequest request) {
        RestClient.RequestBodySpec post = restClient.post()
                .uri("/leads")
                .contentType(MediaType.APPLICATION_JSON);

        if (!apiKey.isBlank()) {
            post = post.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        try {
            post.body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                return;
            }
            throw new ConversionFlowIntegrationException("conversionflow_lead_sync_failed", exception);
        } catch (RestClientException exception) {
            throw new ConversionFlowIntegrationException("conversionflow_lead_sync_failed", exception);
        }
    }
}
