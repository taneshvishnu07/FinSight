package com.finsight.backend.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs once per request. Reads "Authorization: Bearer <token>", validates the JWT and,
 * if valid, stores the authenticated user in the SecurityContext so controllers can
 * access it.
 *
 * Requests without a token, or with an invalid one, simply continue unauthenticated;
 * SecurityConfig then rejects them on protected routes.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    /**
     * Core filter logic: extract the token, look up the user by the email stored in
     * it, validate it, and set the authentication.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader =
                request.getHeader("Authorization");

        // -----------------------------------------------------
        // No Authorization header
        // -----------------------------------------------------

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // -----------------------------------------------------
        // Extract token
        // -----------------------------------------------------

        final String jwt =
                authHeader.substring(7);

        try {

            // -------------------------------------------------
            // Extract email from JWT
            // -------------------------------------------------

            final String email =
                    jwtService.extractEmail(jwt);

            // -------------------------------------------------
            // Authenticate user
            // -------------------------------------------------

            // Only authenticate if nobody has authenticated this request already.
            if (email != null
                    && SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(email);

                // ---------------------------------------------
                // Validate JWT
                // ---------------------------------------------

                if (jwtService.isTokenValid(
                        jwt,
                        userDetails.getUsername())) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication);
                }
            }

        // Any problem (expired, malformed or tampered token, unknown user) means "not
        // authenticated": clear the context and carry on.
        } catch (Exception e) {

            // Invalid JWT.
            // Do not authenticate the request.
            SecurityContextHolder
                    .clearContext();
        }

        filterChain.doFilter(request, response);
    }
}