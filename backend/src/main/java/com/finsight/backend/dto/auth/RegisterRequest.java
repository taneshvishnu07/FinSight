package com.finsight.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Body of POST /api/auth/register with validation rules: full name = first and last
 * name (letters, spaces, hyphens, apostrophes); valid email; password 8-72 characters
 * with upper-case, lower-case, digit and special character. Keep in sync with the
 * checks in static/js/auth.js.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Full name is required.")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters.")
    @Pattern(
            regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[\\'\\-\\s][A-Za-zÀ-ÖØ-öø-ÿ]+)+$",
            message = "Please enter your first and last name using letters, spaces, hyphens or apostrophes only."
    )
    private String fullName;

    @NotBlank(message = "Email address is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 150, message = "Email address is too long.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "Password must contain uppercase, lowercase, number and special character."
    )
    private String password;

}