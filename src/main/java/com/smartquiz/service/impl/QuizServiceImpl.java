package com.smartquiz.service.impl;

import com.smartquiz.dto.*;
import com.smartquiz.entity.Notification;
import com.smartquiz.entity.Question;
import com.smartquiz.entity.QuizSet;
import com.smartquiz.entity.QuizSubmission;
import com.smartquiz.entity.User;
import com.smartquiz.exception.BadRequestException;
import com.smartquiz.exception.ResourceNotFoundException;
import com.smartquiz.repository.NotificationRepository;
import com.smartquiz.repository.QuizSetRepository;
import com.smartquiz.repository.QuizSubmissionRepository;
import com.smartquiz.repository.UserRepository;
import com.smartquiz.service.FileParserService;
import com.smartquiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizSetRepository quizSetRepository;
    private final UserRepository userRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final NotificationRepository notificationRepository;
    private final FileParserService fileParserService;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<QuizSetDto> getAllQuizzes(String category, String difficulty, String search) {
        log.info("Fetching quiz sets with filters - category: {}, difficulty: {}, search: {}", category, difficulty, search);

        Query query = new Query();
        if (category != null && !category.isBlank()) {
            query.addCriteria(Criteria.where("category").regex("^" + Pattern.quote(category.trim()) + "$", "i"));
        }
        if (difficulty != null && !difficulty.isBlank()) {
            query.addCriteria(Criteria.where("difficulty").regex("^" + Pattern.quote(difficulty.trim()) + "$", "i"));
        }
        if (search != null && !search.isBlank()) {
            String s = Pattern.quote(search.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(s, "i"),
                    Criteria.where("description").regex(s, "i")
            ));
        }

        List<QuizSet> quizSets = mongoTemplate.find(query, QuizSet.class);

        return quizSets.stream()
                .map(this::mapToDtoWithoutQuestions)
                .collect(Collectors.toList());
    }

    @Override
    public QuizSetDto getQuizById(String id) {
        log.info("Fetching quiz set details for ID: {}", id);
        QuizSet quizSet = quizSetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuizSet", "id", id));
        return mapToDtoWithQuestions(quizSet);
    }

    @Override
    public QuizSetDto createQuiz(QuizSetDto quizSetDto) {
        log.info("Creating manual quiz set: {}", quizSetDto.getTitle());

        QuizSet quizSet = QuizSet.builder()
                .title(quizSetDto.getTitle().trim())
                .category(quizSetDto.getCategory().trim())
                .description(quizSetDto.getDescription())
                .timeLimitMinutes(quizSetDto.getTimeLimitMinutes() != null ? quizSetDto.getTimeLimitMinutes() : 15)
                .difficulty(quizSetDto.getDifficulty() != null ? quizSetDto.getDifficulty() : "MEDIUM")
                .avgScore(0.0)
                .build();

        if (quizSetDto.getQuestions() != null) {
            for (QuestionDto qDto : quizSetDto.getQuestions()) {
                Question question = Question.builder()
                        .id(UUID.randomUUID().toString())
                        .questionText(qDto.getQuestionText())
                        .optionA(qDto.getOptionA())
                        .optionB(qDto.getOptionB())
                        .optionC(qDto.getOptionC())
                        .optionD(qDto.getOptionD())
                        .correctAnswer(qDto.getCorrectAnswer().toUpperCase())
                        .explanation(qDto.getExplanation())
                        .build();
                quizSet.addQuestion(question);
            }
        }

        quizSet.setTotalQuestions(quizSet.getQuestions().size());
        QuizSet saved = quizSetRepository.save(quizSet);
        return mapToDtoWithQuestions(saved);
    }

    @Override
    public QuizSetDto importQuizFromFile(MultipartFile file, String title, String category, String description, Integer timeLimitMinutes, String difficulty) {
        log.info("Importing quiz from file: {}", file.getOriginalFilename());

        List<Question> parsedQuestions = fileParserService.parseQuestionsFile(file);
        if (parsedQuestions.isEmpty()) {
            throw new BadRequestException("No valid questions could be extracted from the file.");
        }

        String finalTitle = (title != null && !title.isBlank()) ? title.trim() : cleanFileNameToTitle(file.getOriginalFilename());
        String finalCategory = (category != null && !category.isBlank()) ? category.trim() : "General Knowledge";
        String finalDescription = (description != null && !description.isBlank()) ? description.trim() : "Imported from file: " + file.getOriginalFilename();
        int finalTimeLimit = (timeLimitMinutes != null && timeLimitMinutes > 0) ? timeLimitMinutes : Math.max(5, parsedQuestions.size() * 2);
        String finalDifficulty = (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : "MEDIUM";

        QuizSet quizSet = QuizSet.builder()
                .title(finalTitle)
                .category(finalCategory)
                .description(finalDescription)
                .timeLimitMinutes(finalTimeLimit)
                .difficulty(finalDifficulty)
                .totalQuestions(parsedQuestions.size())
                .avgScore(0.0)
                .build();

        for (Question q : parsedQuestions) {
            if (q.getId() == null || q.getId().isBlank()) {
                q.setId(UUID.randomUUID().toString());
            }
            quizSet.addQuestion(q);
        }

        QuizSet saved = quizSetRepository.save(quizSet);
        log.info("Successfully imported and saved QuizSet ID: {} with {} questions", saved.getId(), saved.getQuestions().size());

        return mapToDtoWithQuestions(saved);
    }

    @Override
    public QuizSubmissionResponse submitQuiz(String quizId, QuizSubmissionRequest request) {
        log.info("User {} submitting answers for quiz ID: {}", request.getUserId(), quizId);

        QuizSet quizSet = quizSetRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizSet", "id", quizId));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        List<Question> questions = quizSet.getQuestions();
        if (questions.isEmpty()) {
            throw new BadRequestException("Cannot submit quiz with no questions.");
        }

        int correctCount = 0;
        List<QuizSubmissionResponse.QuestionResult> results = new ArrayList<>();
        Map<String, String> userAnswers = request.getAnswers() != null ? request.getAnswers() : Map.of();

        for (Question question : questions) {
            String selected = userAnswers.get(question.getId());
            boolean isCorrect = false;

            if (selected != null && !selected.isBlank()) {
                String sel = selected.trim();
                // 1. Direct letter match (e.g., "A", "B", "C", "D")
                if (sel.equalsIgnoreCase(question.getCorrectAnswer())) {
                    isCorrect = true;
                }
                // 2. Direct text match with correctAnswerText (e.g., "Dart")
                else if (question.getCorrectAnswerText() != null && sel.equalsIgnoreCase(question.getCorrectAnswerText().trim())) {
                    isCorrect = true;
                }
                // 3. User submitted letter, check if that option's text equals correctAnswerText
                else {
                    String chosenOptionText = switch (sel.toUpperCase()) {
                        case "A" -> question.getOptionA();
                        case "B" -> question.getOptionB();
                        case "C" -> question.getOptionC();
                        case "D" -> question.getOptionD();
                        default -> null;
                    };
                    if (chosenOptionText != null && question.getCorrectAnswerText() != null &&
                            chosenOptionText.trim().equalsIgnoreCase(question.getCorrectAnswerText().trim())) {
                        isCorrect = true;
                    }
                }
            }

            if (isCorrect) {
                correctCount++;
            }

            results.add(QuizSubmissionResponse.QuestionResult.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .selectedAnswer(selected)
                    .correctAnswer(question.getCorrectAnswer())
                    .isCorrect(isCorrect)
                    .explanation(question.getExplanation())
                    .build());
        }

        double scorePercentage = Math.round(((double) correctCount / questions.size() * 100.0) * 10.0) / 10.0;

        // Save submission
        QuizSubmission submission = QuizSubmission.builder()
                .userId(user.getId())
                .quizSetId(quizSet.getId())
                .score(scorePercentage)
                .correctCount(correctCount)
                .totalQuestions(questions.size())
                .build();
        submission = submissionRepository.save(submission);

        // Update User Streak
        int updatedStreak = user.getCurrentStreak() + 1;
        user.setCurrentStreak(updatedStreak);
        userRepository.save(user);

        // Recalculate QuizSet Average Score
        List<QuizSubmission> allSubmissions = submissionRepository.findByQuizSetId(quizId);
        double newAvg = allSubmissions.stream()
                .mapToDouble(QuizSubmission::getScore)
                .average()
                .orElse(scorePercentage);
        quizSet.setAvgScore(Math.round(newAvg * 10.0) / 10.0);
        quizSetRepository.save(quizSet);

        // Generate achievement / reminder notification
        Notification notice = Notification.builder()
                .title("Quiz Completed: " + quizSet.getTitle())
                .message("You scored " + scorePercentage + "% (" + correctCount + "/" + questions.size() +
                        ")! Your current learning streak is now " + updatedStreak + " days 🔥.")
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .userId(user.getId())
                .build();
        notificationRepository.save(notice);

        return QuizSubmissionResponse.builder()
                .submissionId(submission.getId())
                .quizSetId(quizSet.getId())
                .quizTitle(quizSet.getTitle())
                .userId(user.getId())
                .scorePercentage(scorePercentage)
                .correctCount(correctCount)
                .totalQuestions(questions.size())
                .updatedStreak(updatedStreak)
                .submittedAt(submission.getSubmittedAt() != null ? submission.getSubmittedAt() : LocalDateTime.now())
                .questionResults(results)
                .build();
    }

    @Override
    public void deleteQuiz(String id) {
        if (!quizSetRepository.existsById(id)) {
            throw new ResourceNotFoundException("QuizSet", "id", id);
        }
        quizSetRepository.deleteById(id);
    }

    private String cleanFileNameToTitle(String filename) {
        if (filename == null) return "Imported Quiz";
        int dot = filename.lastIndexOf('.');
        String base = (dot > 0) ? filename.substring(0, dot) : filename;
        return base.replace('_', ' ').replace('-', ' ').trim();
    }

    private QuizSetDto mapToDtoWithoutQuestions(QuizSet quizSet) {
        return QuizSetDto.builder()
                .id(quizSet.getId())
                .title(quizSet.getTitle())
                .category(quizSet.getCategory())
                .description(quizSet.getDescription())
                .totalQuestions(quizSet.getTotalQuestions())
                .timeLimitMinutes(quizSet.getTimeLimitMinutes())
                .difficulty(quizSet.getDifficulty())
                .avgScore(quizSet.getAvgScore())
                .createdAt(quizSet.getCreatedAt())
                .questions(new ArrayList<>())
                .build();
    }

    private QuizSetDto mapToDtoWithQuestions(QuizSet quizSet) {
        QuizSetDto dto = mapToDtoWithoutQuestions(quizSet);
        if (quizSet.getQuestions() != null) {
            dto.setQuestions(quizSet.getQuestions().stream()
                    .map(this::mapQuestionToDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private QuestionDto mapQuestionToDto(Question q) {
        return QuestionDto.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .optionA(q.getOptionA())
                .optionB(q.getOptionB())
                .optionC(q.getOptionC())
                .optionD(q.getOptionD())
                .correctAnswer(q.getCorrectAnswer())
                .correctAnswerText(q.getCorrectAnswerText())
                .explanation(q.getExplanation())
                .build();
    }
}
