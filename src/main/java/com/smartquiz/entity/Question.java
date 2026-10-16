package com.smartquiz.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String questionText;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private String correctAnswer; // "A", "B", "C", or "D"

    private String explanation;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
