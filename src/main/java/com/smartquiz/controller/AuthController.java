package com.smartquiz.controller;

import com.smartquiz.dto.ApiResponse;
import com.smartquiz.dto.AuthResponse;
import com.smartquiz.dto.LoginRequest;
import com.smartquiz.dto.RegisterRequest;
import com.smartquiz.dto.UserResponse;
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
@Tag(name = "Authentication", description = "Core authentication APIs: register, login with JWT Token, and user profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "1. Register Account",
            description = "Registers user account directly with name, email, and password. Returns user profile and JWT Bearer token."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authResponse, "Account registered successfully!"));
    }

    @PostMapping("/login")
    @Operation(
            summary = "2. Login",
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
