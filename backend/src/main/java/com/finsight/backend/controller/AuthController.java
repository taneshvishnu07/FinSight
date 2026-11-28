package com.finsight.backend.controller;

import com.finsight.backend.dto.auth.LoginRequest;
import com.finsight.backend.dto.auth.ChangePasswordRequest;
import com.finsight.backend.dto.auth.LoginResponse;
import com.finsight.backend.dto.auth.RegisterRequest;
import com.finsight.backend.service.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints (no JWT needed except for changing the password),
 * mounted under /api/auth. Request bodies are validated with Bean Validation (@Valid).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register - create a new account. Responds 201 on success.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Registration successful.");

    }

    /**
     * POST /api/auth/login - verify email/password and return a JWT with basic user
     * info and whether the financial profile has been completed.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request));

    }


    /**
     * PATCH /api/auth/password - change the password of the logged-in user. Requires
     * a valid JWT (this path is not in SecurityConfig's public list); the service also
     * verifies the current password.
     */
    @PatchMapping("/password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully.");
    }

}