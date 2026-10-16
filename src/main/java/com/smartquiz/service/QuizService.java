package com.smartquiz.service;

import com.smartquiz.dto.QuizSetDto;
import com.smartquiz.dto.QuizSubmissionRequest;
import com.smartquiz.dto.QuizSubmissionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuizService {

    List<QuizSetDto> getAllQuizzes(String category, String difficulty, String search);

    QuizSetDto getQuizById(Long id);

    QuizSetDto createQuiz(QuizSetDto quizSetDto);

    QuizSetDto importQuizFromFile(MultipartFile file, String title, String category, String description, Integer timeLimitMinutes, String difficulty);

    QuizSubmissionResponse submitQuiz(Long quizId, QuizSubmissionRequest request);

    void deleteQuiz(Long id);
}
