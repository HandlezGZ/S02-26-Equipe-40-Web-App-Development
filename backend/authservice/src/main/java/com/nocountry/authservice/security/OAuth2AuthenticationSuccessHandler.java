package com.nocountry.authservice.security;

import com.nocountry.authservice.dto.AuthTokenResponse;
import com.nocountry.authservice.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String successRedirectUri;

    public OAuth2AuthenticationSuccessHandler(
            AuthService authService,
            @Value("${app.oauth2.success-redirect-uri}") String successRedirectUri
    ) {
        this.authService = authService;
        this.successRedirectUri = successRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String subject = principal.getAttribute("sub");

        if (email == null || subject == null) {
            response.sendRedirect(
                    UriComponentsBuilder.fromUriString(successRedirectUri)
                            .queryParam("error", "google_profile_incomplete")
                            .build()
                            .toUriString()
            );
            return;
        }

        AuthTokenResponse token = authService.loginWithGoogle(email, subject);

        String redirect = UriComponentsBuilder.fromUriString(successRedirectUri)
                .queryParam("accessToken", token.accessToken())
                .queryParam("tokenType", token.tokenType())
                .queryParam("expiresIn", token.expiresIn())
                .queryParam("userId", token.userId())
                .queryParam("email", token.email())
                .build()
                .toUriString();

        response.sendRedirect(redirect);
    }
}
