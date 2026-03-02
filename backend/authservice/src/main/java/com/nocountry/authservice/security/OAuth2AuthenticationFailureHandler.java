package com.nocountry.authservice.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String failureRedirectUri;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.failure-redirect-uri}") String failureRedirectUri
    ) {
        this.failureRedirectUri = failureRedirectUri;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String redirect = UriComponentsBuilder.fromUriString(failureRedirectUri)
                .queryParam("error", "google_auth_failed")
                .build()
                .toUriString();

        response.sendRedirect(redirect);
    }
}
