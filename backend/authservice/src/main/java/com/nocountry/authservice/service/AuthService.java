package com.nocountry.authservice.service;

import com.nocountry.authservice.domain.AuthProvider;
import com.nocountry.authservice.domain.User;
import com.nocountry.authservice.dto.AuthTokenResponse;
import com.nocountry.authservice.dto.LoginRequest;
import com.nocountry.authservice.dto.RegisterRequest;
import com.nocountry.authservice.integration.conversionflow.ConversionFlowLeadClient;
import com.nocountry.authservice.integration.conversionflow.CreateLeadRequest;
import com.nocountry.authservice.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ConversionFlowLeadClient conversionFlowLeadClient;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ConversionFlowLeadClient conversionFlowLeadClient
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.conversionFlowLeadClient = conversionFlowLeadClient;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        Optional<User> existing = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            User existingUser = existing.get();
            if (existingUser.getPasswordHash() == null || !passwordEncoder.matches(request.password(), existingUser.getPasswordHash())) {
                throw new IllegalArgumentException("email_already_registered");
            }

            conversionFlowLeadClient.createLead(new CreateLeadRequest(
                    existingUser.getId().toString(),
                    existingUser.getEmail(),
                    request.gclid(),
                    request.fbclid(),
                    request.fbp(),
                    request.fbc(),
                    request.utmSource(),
                    request.utmCampaign()
            ));

            String replayToken = jwtService.generateAccessToken(existingUser);
            return new AuthTokenResponse(
                    replayToken,
                    "Bearer",
                    jwtService.getExpirationSeconds(),
                    existingUser.getId().toString(),
                    existingUser.getEmail()
            );
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL);

        User savedUser = userRepository.save(user);
        conversionFlowLeadClient.createLead(new CreateLeadRequest(
                savedUser.getId().toString(),
                savedUser.getEmail(),
                request.gclid(),
                request.fbclid(),
                request.fbp(),
                request.fbc(),
                request.utmSource(),
                request.utmCampaign()
        ));
        String token = jwtService.generateAccessToken(savedUser);

        return new AuthTokenResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                savedUser.getId().toString(),
                savedUser.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("invalid_credentials"));

        if (user.getPasswordHash() == null || user.getProvider() == AuthProvider.GOOGLE) {
            throw new BadCredentialsException("password_login_not_available");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("invalid_credentials");
        }

        String token = jwtService.generateAccessToken(user);

        return new AuthTokenResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId().toString(),
                user.getEmail()
        );
    }

    @Transactional
    public AuthTokenResponse loginWithGoogle(String email, String googleSubject) {
        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> byGoogleSubject = userRepository.findByGoogleSubject(googleSubject);
        User user = byGoogleSubject.orElse(null);
        boolean createdNewUser = false;

        if (user == null) {
            Optional<User> byEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
            if (byEmail.isPresent()) {
                user = byEmail.get();
                user.setGoogleSubject(googleSubject);
            } else {
                user = new User();
                user.setEmail(normalizedEmail);
                user.setProvider(AuthProvider.GOOGLE);
                user.setGoogleSubject(googleSubject);
                createdNewUser = true;
            }
            user = userRepository.save(user);
        }

        if (createdNewUser) {
            conversionFlowLeadClient.createLead(new CreateLeadRequest(
                    user.getId().toString(),
                    user.getEmail(),
                    null,
                    null,
                    null,
                    null,
                    "google",
                    null
            ));
        }

        String token = jwtService.generateAccessToken(user);

        return new AuthTokenResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId().toString(),
                user.getEmail()
        );
    }
}
