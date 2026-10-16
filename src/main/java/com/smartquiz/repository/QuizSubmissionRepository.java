package com.smartquiz.repository;

import com.smartquiz.entity.QuizSubmission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSubmissionRepository extends MongoRepository<QuizSubmission, String> {

    List<QuizSubmission> findByUserId(String userId);

    List<QuizSubmission> findByQuizSetId(String quizSetId);
}
