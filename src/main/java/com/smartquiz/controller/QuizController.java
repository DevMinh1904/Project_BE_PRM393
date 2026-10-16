package com.smartquiz.controller;

import com.smartquiz.dto.ApiResponse;
import com.smartquiz.dto.QuizSetDto;
import com.smartquiz.dto.QuizSubmissionRequest;
import com.smartquiz.dto.QuizSubmissionResponse;
import com.smartquiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Quizzes", description = "Endpoints for managing quiz sets, importing via Excel/CSV, retrieving questions, and submitting quiz results")
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    @Operation(summary = "Get all quiz sets", description = "Retrieves all available quiz sets for the dashboard/library view with optional search, category, and difficulty filters.")
    public ResponseEntity<ApiResponse<List<QuizSetDto>>> getAllQuizzes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String search) {
        List<QuizSetDto> quizzes = quizService.getAllQuizzes(category, difficulty, search);
        return ResponseEntity.ok(ApiResponse.ok(quizzes, "Quizzes fetched successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get quiz details with questions", description = "Retrieves detailed quiz metadata along with all its multiple choice questions.")
    public ResponseEntity<ApiResponse<QuizSetDto>> getQuizById(@PathVariable String id) {
        QuizSetDto quiz = quizService.getQuizById(id);
        return ResponseEntity.ok(ApiResponse.ok(quiz, "Quiz details retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create quiz set manually", description = "Creates a new QuizSet with questions directly from JSON.")
    public ResponseEntity<ApiResponse<QuizSetDto>> createQuiz(@Valid @RequestBody QuizSetDto quizSetDto) {
        QuizSetDto created = quizService.createQuiz(quizSetDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Quiz created successfully"));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import quiz from Excel (.xlsx) or CSV file",
            description = "Accepts a multipart .xlsx or .csv file up to 15MB, parses rows using Apache POI, and creates a new QuizSet in MongoDB."
    )
    public ResponseEntity<ApiResponse<QuizSetDto>> importQuiz(
            @Parameter(description = "Excel (.xlsx, .xls) or CSV file containing questions", required = true,
                       content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                       schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "timeLimitMinutes", required = false) Integer timeLimitMinutes,
            @RequestParam(value = "difficulty", required = false) String difficulty
    ) {
        QuizSetDto imported = quizService.importQuizFromFile(file, title, category, description, timeLimitMinutes, difficulty);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(imported, "Quiz imported and saved successfully with " + imported.getTotalQuestions() + " questions"));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit quiz answers for evaluation", description = "Submits user's answers for a quiz, computes score percentage, updates user's streak, and generates a notification alert.")
    public ResponseEntity<ApiResponse<QuizSubmissionResponse>> submitQuiz(
            @PathVariable String id,
            @Valid @RequestBody QuizSubmissionRequest request) {
        QuizSubmissionResponse response = quizService.submitQuiz(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Quiz evaluated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete quiz set", description = "Deletes a quiz set and cascades removal to all its questions.")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable String id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Quiz set deleted successfully"));
    }
}
