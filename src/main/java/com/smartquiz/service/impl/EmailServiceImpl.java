package com.smartquiz.service.impl;

import com.smartquiz.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@smartquiz.com}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String fullName, String otpCode) {
        log.info("==========================================================");
        log.info("📧 [SmartQuiz Email OTP Dispatcher]");
        log.info("Recipient: {} <{}>", fullName, toEmail);
        log.info("Your Registration OTP Code: >>> {} <<<", otpCode);
        log.info("Expiration: 5 minutes");
        log.info("==========================================================");

        if (mailSender == null) {
            log.info("JavaMailSender not configured. OTP printed to console for local testing.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("SmartQuiz - Email Verification Code: " + otpCode);

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #fafafa;">
                        <h2 style="color: #4F46E5; margin-bottom: 8px;">SmartQuiz Mobile App</h2>
                        <h3 style="color: #111827; margin-top: 0;">Verify Your Email Address</h3>
                        <p style="color: #4b5563; font-size: 15px;">Hello <strong>%s</strong>,</p>
                        <p style="color: #4b5563; font-size: 15px;">Thank you for registering on SmartQuiz! Please enter the 6-digit verification code below in your mobile app to activate your account:</p>
                        <div style="text-align: center; margin: 32px 0;">
                            <span style="display: inline-block; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #4F46E5; background-color: #EEF2FF; padding: 14px 28px; border-radius: 8px; border: 1px dashed #6366F1;">
                                %s
                            </span>
                        </div>
                        <p style="color: #6b7280; font-size: 14px;">This code will expire in <strong>5 minutes</strong>. If you did not initiate this request, please safely disregard this email.</p>
                        <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;" />
                        <p style="color: #9ca3af; font-size: 12px; text-align: center;">SmartQuiz PRM393 &copy; 2026. All rights reserved.</p>
                    </div>
                    """.formatted(fullName, otpCode);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Verification email successfully delivered via SMTP to {}", toEmail);

        } catch (Exception e) {
            log.warn("Could not dispatch email via SMTP (local dev mode active). " +
                     "Reason: {}. Use console OTP code [{}] for testing.", e.getMessage(), otpCode);
        }
    }
}
