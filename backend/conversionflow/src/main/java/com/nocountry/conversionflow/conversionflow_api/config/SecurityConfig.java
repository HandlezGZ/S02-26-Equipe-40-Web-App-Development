package com.nocountry.conversionflow.conversionflow_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Desativa CSRF para permitir POST via Postman
                .csrf(csrf -> csrf.disable())

                // Configuração de autorização
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/leads/**",
                                "/checkout",
                                "/stripe/webhook"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Mantém suporte a Basic Auth para rotas protegidas
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
