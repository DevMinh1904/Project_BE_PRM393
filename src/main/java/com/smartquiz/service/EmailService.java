package com.smartquiz.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String fullName, String otpCode);
}
