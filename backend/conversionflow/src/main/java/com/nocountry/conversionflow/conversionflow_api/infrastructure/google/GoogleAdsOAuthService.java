package com.nocountry.conversionflow.conversionflow_api.infrastructure.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nocountry.conversionflow.conversionflow_api.config.properties.GoogleAdsApiProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

/**
 * Obtém access token do Google via refresh token.
 *
 * Endpoint OAuth2 token:
 *   POST https://oauth2.googleapis.com/token
 */
@Component
public class GoogleAdsOAuthService {

    private final GoogleAdsApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private volatile String cachedAccessToken;
    private volatile Instant cachedExpiry;

    public GoogleAdsOAuthService(GoogleAdsApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String getAccessToken() {

        // 1) Se o usuário forneceu token manual, usa direto.
        if (properties.getAccessToken() != null && !properties.getAccessToken().isBlank()) {
            return properties.getAccessToken().trim();
        }

        // 2) Caso exista cache válido, retorna.
        Instant now = Instant.now();
        if (cachedAccessToken != null && cachedExpiry != null && now.isBefore(cachedExpiry.minusSeconds(60))) {
            return cachedAccessToken;
        }

        synchronized (this) {
            now = Instant.now();
            if (cachedAccessToken != null && cachedExpiry != null && now.isBefore(cachedExpiry.minusSeconds(60))) {
                return cachedAccessToken;
            }

            if (isBlank(properties.getOauthClientId()) || isBlank(properties.getOauthClientSecret()) || isBlank(properties.getOauthRefreshToken())) {
                throw new IllegalStateException(
                        "Google Ads OAuth não configurado. Informe google.ads.api.oauth-client-id, oauth-client-secret e oauth-refresh-token (ou google.ads.api.access-token para MVP)."
                );
            }

            String url = "https://oauth2.googleapis.com/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", properties.getOauthClientId());
            form.add("client_secret", properties.getOauthClientSecret());
            form.add("refresh_token", properties.getOauthRefreshToken());
            form.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Falha ao obter access token Google OAuth. status=" + response.getStatusCode() + " body=" + response.getBody());
            }

            try {
                JsonNode node = objectMapper.readTree(response.getBody());
                String token = node.path("access_token").asText(null);
                long expiresIn = node.path("expires_in").asLong(0);
                if (token == null || token.isBlank()) {
                    throw new IllegalStateException("Resposta OAuth sem access_token. body=" + response.getBody());
                }

                cachedAccessToken = token;
                cachedExpiry = Instant.now().plusSeconds(Math.max(60, expiresIn));
                return cachedAccessToken;
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao parsear resposta OAuth. body=" + response.getBody(), e);
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
