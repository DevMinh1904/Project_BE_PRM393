package com.smartquiz.scheduler;

import com.smartquiz.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpCleanupScheduler {

    private final OtpVerificationRepository otpVerificationRepository;

    /**
     * Automatically purges expired OTP verification records from the database every day at midnight (00:00:00).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupExpiredOtpsDaily() {
        log.info("Starting scheduled daily cleanup of expired OTPs...");
        try {
            int deletedCount = otpVerificationRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
            log.info("Daily OTP cleanup completed: removed {} expired OTP record(s) from database.", deletedCount);
        } catch (Exception e) {
            log.error("Failed to execute daily OTP cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Initial cleanup 10 seconds after server boot to purge any records expired during downtime.
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void cleanupExpiredOtpsOnStartup() {
        try {
            int deletedCount = otpVerificationRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
            if (deletedCount > 0) {
                log.info("Startup OTP cleanup: purged {} stale OTP record(s).", deletedCount);
            }
        } catch (Exception e) {
            log.warn("Startup OTP cleanup encountered an issue: {}", e.getMessage());
        }
    }
}
