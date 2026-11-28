package com.finsight.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Body of PATCH /api/auth/password. New password: 6-100 characters, and it must match
 * `confirmPassword` (checked in AuthServiceImpl).
 */
@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
