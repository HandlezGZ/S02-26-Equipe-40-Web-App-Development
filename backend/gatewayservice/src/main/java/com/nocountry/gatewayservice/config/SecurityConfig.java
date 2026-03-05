package com.nocountry.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/health", "/health/**").permitAll()
                        .pathMatchers("/api/v1/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .pathMatchers("/internal/**", "/admin/**", "/actuator/**").hasRole("ADMIN")
                        .anyExchange().denyAll())
                .build();
    }

    @Bean
    MapReactiveUserDetailsService userDetailsService(
            @Value("${app.security.admin-user:gateway-admin}") String adminUser,
            @Value("${app.security.admin-pass:change-me}") String adminPass,
            PasswordEncoder passwordEncoder
    ) {
        UserDetails admin = User.withUsername(adminUser)
                .password(passwordEncoder.encode(adminPass))
                .roles("ADMIN")
                .build();
        return new MapReactiveUserDetailsService(admin);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
