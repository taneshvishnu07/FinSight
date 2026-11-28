package com.finsight.backend.service;

import com.finsight.backend.dto.auth.LoginRequest;
import com.finsight.backend.dto.auth.ChangePasswordRequest;
import com.finsight.backend.dto.auth.LoginResponse;
import com.finsight.backend.dto.auth.RegisterRequest;

/**
 * Account operations: register, login and change password. Implementation:
 * AuthServiceImpl.
 */
public interface AuthService {

    /**
     * Create a new user account (email must be unused).
     */
    void register(RegisterRequest request);

    /**
     * Verify credentials and return a JWT.
     */
    LoginResponse login(LoginRequest request);

    /**
     * Change the logged-in user's password.
     */
    void changePassword(ChangePasswordRequest request);

}