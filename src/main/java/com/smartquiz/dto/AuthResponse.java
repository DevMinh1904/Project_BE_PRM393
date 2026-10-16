package com.smartquiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private String id;
    private String fullName;
    private String email;
    private Integer currentStreak;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
