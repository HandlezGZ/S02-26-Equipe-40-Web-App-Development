package com.nocountry.authservice.controller;

import com.nocountry.authservice.config.ApiPaths;
import com.nocountry.authservice.dto.AuthTokenResponse;
import com.nocountry.authservice.dto.LoginRequest;
import com.nocountry.authservice.dto.RegisterRequest;
import com.nocountry.authservice.integration.conversionflow.ConversionFlowIntegrationException;
import com.nocountry.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({ApiPaths.PUBLIC_AUTH_V1, ApiPaths.LEGACY_AUTH})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokenResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google/url")
    public ResponseEntity<Map<String, String>> googleAuthorizationUrl() {
        return ResponseEntity.ok(Map.of("url", "/oauth2/authorization/google"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ConversionFlowIntegrationException.class)
    public ResponseEntity<Map<String, String>> handleLeadIntegrationFailure(ConversionFlowIntegrationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", exception.getMessage()));
    }
}
