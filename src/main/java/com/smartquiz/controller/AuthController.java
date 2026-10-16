package com.smartquiz.controller;

import com.smartquiz.dto.*;
import com.smartquiz.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Core authentication APIs: send-otp, register with OTP, and login with JWT Token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    @Operation(
            summary = "1. Send OTP to Email",
            description = "Takes user email, validates it is not already registered, generates a 6-digit verification code, and dispatches it to the email."
    )
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Verification OTP has been sent to " + request.getEmail() + ". Please enter it to complete registration."));
    }

    @PostMapping("/register")
    @Operation(
            summary = "2. Register Account with OTP",
            description = "Registers user account by verifying the 6-digit OTP code along with name, email, and password. Returns user profile and JWT Bearer token."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authResponse, "Account registered and verified successfully!"));
    }

    @PostMapping("/login")
    @Operation(
            summary = "3. Login",
            description = "Verifies email and password credentials, returning the user profile and JWT Bearer token for BearerAuth."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "Login successful"));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile", description = "Fetches the current user profile and streak details by user ID.")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@PathVariable String userId) {
        UserResponse userResponse = authService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(userResponse, "User profile retrieved successfully"));
    }
}
