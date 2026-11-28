package com.finsight.backend.config;

import com.finsight.backend.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationProvider;

import org.springframework.security.config.Customizer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Main Spring Security setup: a stateless, JWT-based API.
 *
 * - No server sessions and no CSRF tokens (authentication travels in the Authorization header, not a cookie).
 * - /api/auth/** (login, register) and the static frontend files are public.
 * - Every other request must carry a valid JWT.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // Filter that reads the JWT and signs the user in for the current request.
    private final JwtAuthenticationFilter jwtFilter;

    private final AuthenticationProvider authenticationProvider;


    /**
     * Defines the HTTP security rules. The JWT filter runs before Spring's
     * username/password filter so authenticated requests are recognised first.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Authentication and frontend static resources do not require JWT
                        .requestMatchers(
                                "/api/auth/**",
                                "/error",
                                "/",
                                "/index.html",
                                "/*.html",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/favicon.ico",
                                "/sample-transactions-template.csv"
                        ).permitAll()

                        // Everything else requires JWT
                        .anyRequest().authenticated()
                )

                .authenticationProvider(
                        authenticationProvider
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}