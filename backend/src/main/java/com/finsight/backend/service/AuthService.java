package com.finsight.backend.service;

import com.finsight.backend.dto.auth.LoginRequest;
import com.finsight.backend.dto.auth.ChangePasswordRequest;
import com.finsight.backend.dto.auth.LoginResponse;
import com.finsight.backend.dto.auth.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void changePassword(ChangePasswordRequest request);

}