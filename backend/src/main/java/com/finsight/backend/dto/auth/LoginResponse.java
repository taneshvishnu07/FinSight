package com.finsight.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Login result: the JWT, basic user info, and `profileCompleted` so the frontend knows
 * whether to show profile setup.
 */
@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String fullName;
    private String email;
    private boolean profileCompleted;

}