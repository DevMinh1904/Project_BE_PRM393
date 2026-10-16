package com.smartquiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSetDto {

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    private String description;

    @Builder.Default
    private Integer totalQuestions = 0;

    @Builder.Default
    private Integer timeLimitMinutes = 15;

    @Builder.Default
    private String difficulty = "MEDIUM";

    @Builder.Default
    private Double avgScore = 0.0;

    private LocalDateTime createdAt;

    @Builder.Default
    private List<QuestionDto> questions = new ArrayList<>();
}
