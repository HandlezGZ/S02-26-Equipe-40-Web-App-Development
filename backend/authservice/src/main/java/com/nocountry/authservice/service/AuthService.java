package com.nocountry.authservice.service;

import com.nocountry.authservice.domain.AuthProvider;
import com.nocountry.authservice.domain.User;
import com.nocountry.authservice.dto.AuthTokenResponse;
import com.nocountry.authservice.dto.LoginRequest;
import com.nocountry.authservice.dto.RegisterRequest;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("email_already_registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL);

        User savedUser = userRepository.save(user);
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
        User user;

        if (byGoogleSubject.isPresent()) {
            user = byGoogleSubject.get();
        } else {
            user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .map(existing -> {
                        existing.setGoogleSubject(googleSubject);
                        return existing;
                    })
                    .orElseGet(() -> {
                        User createdUser = new User();
                        createdUser.setEmail(normalizedEmail);
                        createdUser.setProvider(AuthProvider.GOOGLE);
                        createdUser.setGoogleSubject(googleSubject);
                        return createdUser;
                    });
            user = userRepository.save(user);
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
