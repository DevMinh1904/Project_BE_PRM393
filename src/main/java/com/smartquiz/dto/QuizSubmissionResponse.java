package com.smartquiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionResponse {

    private String submissionId;
    private String quizSetId;
    private String quizTitle;
    private String userId;
    private Double scorePercentage;
    private Integer correctCount;
    private Integer totalQuestions;
    private Integer updatedStreak;
    private LocalDateTime submittedAt;
    private List<QuestionResult> questionResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResult {
        private String questionId;
        private String questionText;
        private String selectedAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private String explanation;
    }
}
