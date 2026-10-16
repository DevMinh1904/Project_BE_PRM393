package com.smartquiz.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String otpCode;

    private LocalDateTime expiryTime;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
