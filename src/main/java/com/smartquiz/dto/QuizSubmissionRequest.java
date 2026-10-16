package com.smartquiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    /**
     * Map of questionId -> user selected answer ("A", "B", "C", "D")
     */
    @NotNull(message = "Answers map is required")
    private Map<String, String> answers;
}
