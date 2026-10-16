package com.smartquiz.service.impl;

import com.smartquiz.dto.*;
import com.smartquiz.entity.Notification;
import com.smartquiz.entity.OtpVerification;
import com.smartquiz.entity.User;
import com.smartquiz.exception.BadRequestException;
import com.smartquiz.exception.ResourceNotFoundException;
import com.smartquiz.repository.NotificationRepository;
import com.smartquiz.repository.OtpVerificationRepository;
import com.smartquiz.repository.UserRepository;
import com.smartquiz.service.AuthService;
import com.smartquiz.service.EmailService;
import com.smartquiz.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final JwtService jwtService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void sendOtp(SendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Sending registration OTP to email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email '" + email + "' is already registered. Please log in directly.");
        }

        String otp = generateNumericOtp();

        OtpVerification verification = otpVerificationRepository.findByEmail(email)
                .orElse(OtpVerification.builder().email(email).build());

        verification.setOtpCode(otp);
        verification.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpVerificationRepository.save(verification);

        emailService.sendOtpEmail(email, "Prospective Learner", otp);
        log.info("Registration OTP dispatched to {}", email);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Registering user with email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email '" + email + "' is already registered. Please log in directly.");
        }

        // 1. Verify OTP against OtpVerification record
        OtpVerification verification = otpVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No OTP requested for '" + email + "'. Please request an OTP first."));

        if (LocalDateTime.now().isAfter(verification.getExpiryTime())) {
            throw new BadRequestException("The OTP code has expired. Please request a new OTP code.");
        }

        if (!verification.getOtpCode().equals(request.getOtp().trim())) {
            throw new BadRequestException("Invalid OTP code. Please enter the correct 6-digit code sent to your email.");
        }

        // 2. Consume OTP (single use)
        otpVerificationRepository.delete(verification);

        // 3. Create active User
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(request.getPassword())
                .currentStreak(0)
                .isVerified(true)
                .build();

        User savedUser = userRepository.save(user);

        // 4. Send welcome notification
        Notification welcomeNotice = Notification.builder()
                .title("Welcome to SmartQuiz! 🚀")
                .message("Welcome " + savedUser.getFullName() + "! Your account has been registered and verified. Start learning today!")
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .userId(savedUser.getId())
                .build();
        notificationRepository.save(welcomeNotice);

        // 5. Generate JWT token
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

        if (!user.getPassword().equals(request.getPassword())) {
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

    private String generateNumericOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
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
