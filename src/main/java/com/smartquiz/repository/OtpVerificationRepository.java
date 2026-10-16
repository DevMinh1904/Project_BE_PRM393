package com.smartquiz.repository;

import com.smartquiz.entity.OtpVerification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends MongoRepository<OtpVerification, String> {

    Optional<OtpVerification> findByEmail(String email);

    void deleteByEmail(String email);

    int deleteByExpiryTimeBefore(LocalDateTime cutoff);
}
