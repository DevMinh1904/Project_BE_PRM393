package com.smartquiz.repository;

import com.smartquiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizSetId(Long quizSetId);

    void deleteByQuizSetId(Long quizSetId);
}
