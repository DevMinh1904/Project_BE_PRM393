package com.smartquiz.repository;

import com.smartquiz.entity.QuizSet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSetRepository extends MongoRepository<QuizSet, String> {

    List<QuizSet> findByCategoryIgnoreCase(String category);

    List<QuizSet> findByDifficultyIgnoreCase(String difficulty);
}
