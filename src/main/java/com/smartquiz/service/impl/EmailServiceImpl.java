package com.smartquiz.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquiz.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.mail.username:noreply@smartquiz.com}")
    private String fromEmail;

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${resend.from-email:${RESEND_FROM_EMAIL:SmartQuiz <onboarding@resend.dev>}}")
    private String resendFromEmail;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${brevo.sender-email:${BREVO_SENDER_EMAIL:devminhprogram@gmail.com}}")
    private String brevoSenderEmail;

    @Override
    public void sendOtpEmail(String toEmail, String fullName, String otpCode) {
        log.info("==========================================================");
        log.info("📧 [SmartQuiz Email OTP Dispatcher]");
        log.info("Recipient: {} <{}>", fullName, toEmail);
        log.info("Your Registration OTP Code: >>> {} <<<", otpCode);
        log.info("Expiration: 5 minutes");
        log.info("==========================================================");

        String htmlContent = buildOtpHtmlContent(fullName, otpCode);

        // 1. Try Brevo HTTPS API (Can send to ANY email in the world)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            boolean sent = sendViaBrevo(toEmail, otpCode, htmlContent);
            if (sent) return;
        }

        // 2. Try Resend HTTPS API
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            boolean sent = sendViaResend(toEmail, otpCode, htmlContent);
            if (sent) return;
        }

        // 3. Fallback to JavaMailSender if configured
        if (mailSender == null) {
            log.info("No active mail sender configured. OTP printed to console for local testing.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("SmartQuiz - Email Verification Code: " + otpCode);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Verification email successfully delivered via SMTP to {}", toEmail);

        } catch (Exception e) {
            log.warn("Could not dispatch email via SMTP (local dev mode active). " +
                     "Reason: {}. Use console OTP code [{}] for testing.", e.getMessage(), otpCode);
        }
    }

    private boolean sendViaResend(String toEmail, String otpCode, String htmlContent) {
        try {
            log.info("Sending OTP verification email via Resend HTTPS API to {}...", toEmail);
            Map<String, Object> body = Map.of(
                    "from", resendFromEmail,
                    "to", List.of(toEmail),
                    "subject", "SmartQuiz - Email Verification Code: " + otpCode,
                    "html", htmlContent
            );

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Verification email successfully delivered via Resend API to {}. Response: {}", toEmail, response.body());
                return true;
            } else {
                log.warn("Resend API returned non-2xx status code {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch email via Resend HTTPS API: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendViaBrevo(String toEmail, String otpCode, String htmlContent) {
        try {
            log.info("Sending OTP verification email via Brevo HTTPS API to {}...", toEmail);
            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", "SmartQuiz", "email", brevoSenderEmail.trim()),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", "SmartQuiz - Email Verification Code: " + otpCode,
                    "htmlContent", htmlContent
            );

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Verification email successfully delivered via Brevo API to {}. Response: {}", toEmail, response.body());
                return true;
            } else {
                log.warn("Brevo API returned non-2xx status code {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch email via Brevo HTTPS API: {}", e.getMessage());
            return false;
        }
    }

    private String buildOtpHtmlContent(String fullName, String otpCode) {
        return """
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
    }
}
