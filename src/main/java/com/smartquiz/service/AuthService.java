package com.smartquiz.service;

import com.smartquiz.dto.*;

public interface AuthService {

    void sendOtp(SendOtpRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getUserProfile(Long userId);
}
