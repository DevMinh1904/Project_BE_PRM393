package com.smartquiz.repository;

import com.smartquiz.entity.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    List<QuizSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<QuizSubmission> findByQuizSetId(Long quizSetId);
}
