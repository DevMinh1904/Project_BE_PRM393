package com.smartquiz.service;

import com.smartquiz.dto.AuthResponse;
import com.smartquiz.dto.LoginRequest;
import com.smartquiz.dto.RegisterRequest;
import com.smartquiz.dto.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getUserProfile(String userId);
}
