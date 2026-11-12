package com.smartquiz.service.impl;

import com.smartquiz.dto.AuthResponse;
import com.smartquiz.dto.LoginRequest;
import com.smartquiz.dto.RegisterRequest;
import com.smartquiz.dto.UserResponse;
import com.smartquiz.entity.User;
import com.smartquiz.exception.BadRequestException;
import com.smartquiz.exception.ResourceNotFoundException;
import com.smartquiz.repository.UserRepository;
import com.smartquiz.service.AuthService;
import com.smartquiz.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Registering user with email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email '" + email + "' is already registered. Please log in directly.");
        }

        // Check confirm password if provided
        if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and Confirm Password do not match.");
        }

        // Create active User with BCrypt encrypted password (direct registration - no OTP required)
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .currentStreak(0)
                .isVerified(true)
                .build();

        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtService.generateToken(savedUser);

        log.info("User registered successfully with ID: {}. JWT token issued.", savedUser.getId());
        return mapToAuthResponse(savedUser, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Attempting login for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        boolean passwordMatches = false;
        String currentPasswordHash = user.getPassword();

        if (currentPasswordHash != null && (currentPasswordHash.startsWith("$2a$") || currentPasswordHash.startsWith("$2b$") || currentPasswordHash.startsWith("$2y$"))) {
            passwordMatches = passwordEncoder.matches(request.getPassword(), currentPasswordHash);
        } else if (currentPasswordHash != null && currentPasswordHash.equals(request.getPassword())) {
            // Backward compatibility: match legacy plaintext password and automatically upgrade to BCrypt
            passwordMatches = true;
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            log.info("Legacy plaintext password for user {} upgraded to BCrypt hash.", email);
        }

        if (!passwordMatches) {
            throw new BadRequestException("Invalid email or password.");
        }

        String token = jwtService.generateToken(user);
        log.info("User {} logged in successfully. JWT token issued.", user.getEmail());

        return mapToAuthResponse(user, token);
    }

    @Override
    public UserResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .currentStreak(user.getCurrentStreak())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResponse mapToAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .currentStreak(user.getCurrentStreak())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
