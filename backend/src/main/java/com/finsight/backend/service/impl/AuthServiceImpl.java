package com.finsight.backend.service.impl;

import com.finsight.backend.dto.auth.LoginRequest;
import com.finsight.backend.dto.auth.ChangePasswordRequest;
import com.finsight.backend.dto.auth.LoginResponse;
import com.finsight.backend.dto.auth.RegisterRequest;

import com.finsight.backend.entity.User;

import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.repository.FinancialProfileRepository;

import com.finsight.backend.security.JwtService;

import com.finsight.backend.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

/**
 * Registration, login and password change.
 *
 * Emails are trimmed and lower-cased everywhere so "A@x.com" and "a@x.com " are the
 * same account. Passwords are only ever stored as BCrypt hashes.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final FinancialProfileRepository financialProfileRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    /**
     * Create a user. Fails with "Email already exists." if the email is taken. Role
     * and enabled flag use the entity defaults (USER, true).
     */
    @Override
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(
                request.getEmail().trim().toLowerCase())) {

            throw new RuntimeException(
                    "Email already exists.");
        }

        User user = new User();

        user.setFullName(
                request.getFullName());

        user.setEmail(
                request.getEmail().trim().toLowerCase());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()));

        /*
         * Role defaults to USER in the entity.
         *
         * Enabled defaults to true in the entity.
         */
        userRepository.save(user);
    }

    /**
     * Authenticate credentials through Spring Security (throws on a wrong password),
     * then issue a JWT. `profileCompleted` tells the frontend whether to send the user
     * to the profile setup page.
     */
    @Override
    public LoginResponse login(
            LoginRequest request) {

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(

                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.getPassword())
        );

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found.")
                );

        String token =
                jwtService.generateToken(
                        user.getEmail());

        boolean profileCompleted =
                financialProfileRepository.findByUser(user).isPresent();

        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                profileCompleted
        );
    }
    /**
     * Change the logged-in user's password after checking that: the confirmation
     * matches, the new password differs from the current one, and the current password
     * is correct.
     */
    @Override
    public void changePassword(ChangePasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Password data is required.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

        User user = getAuthenticatedUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Load the User for the email stored in the current security context (set by
     * JwtAuthenticationFilter).
     */
    private User getAuthenticatedUser() {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));
    }

}