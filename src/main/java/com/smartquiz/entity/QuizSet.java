package com.smartquiz.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "quiz_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSet {

    @Id
    private String id;

    private String title;

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

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    public void addQuestion(Question question) {
        if (questions == null) {
            questions = new ArrayList<>();
        }
        questions.add(question);
    }

    public void removeQuestion(Question question) {
        if (questions != null) {
            questions.remove(question);
        }
    }
}
