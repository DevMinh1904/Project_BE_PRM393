package com.smartquiz.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "quiz_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSubmission {

    @Id
    private String id;

    private String userId;

    private String quizSetId;

    private Double score; // e.g. 85.5%

    private Integer correctCount;

    private Integer totalQuestions;

    @CreatedDate
    private LocalDateTime submittedAt;
}
