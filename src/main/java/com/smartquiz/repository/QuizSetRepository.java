package com.smartquiz.repository;

import com.smartquiz.entity.QuizSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSetRepository extends JpaRepository<QuizSet, Long> {

    List<QuizSet> findByCategoryIgnoreCase(String category);

    List<QuizSet> findByDifficultyIgnoreCase(String difficulty);

    @Query("SELECT q FROM QuizSet q WHERE " +
           "(:category IS NULL OR LOWER(q.category) = LOWER(:category)) AND " +
           "(:difficulty IS NULL OR LOWER(q.difficulty) = LOWER(:difficulty)) AND " +
           "(:search IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(q.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<QuizSet> searchQuizzes(
            @Param("category") String category,
            @Param("difficulty") String difficulty,
            @Param("search") String search);
}
