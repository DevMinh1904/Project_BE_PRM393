package com.smartquiz.service.impl;

import com.smartquiz.entity.Question;
import com.smartquiz.exception.BadRequestException;
import com.smartquiz.service.FileParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileParserServiceImpl implements FileParserService {

    @Override
    public List<Question> parseQuestionsFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty or missing");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BadRequestException("File name cannot be determined");
        }

        String lowerName = originalFilename.toLowerCase();
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            return parseExcel(file);
        } else if (lowerName.endsWith(".csv")) {
            return parseCsv(file);
        } else {
            throw new BadRequestException("Unsupported file type. Only .xlsx, .xls, and .csv files are supported.");
        }
    }

    private List<Question> parseExcel(MultipartFile file) {
        List<Question> questions = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BadRequestException("Excel workbook does not contain any sheets.");
            }

            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();

            for (int r = firstRowNum; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                List<String> cellValues = new ArrayList<>();
                short minColIx = row.getFirstCellNum();
                short maxColIx = row.getLastCellNum();
                if (minColIx < 0) {
                    continue;
                }

                for (int c = 0; c < maxColIx; c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null) {
                        cellValues.add("");
                    } else {
                        cellValues.add(dataFormatter.formatCellValue(cell).trim());
                    }
                }

                // Check if completely empty
                boolean allEmpty = cellValues.stream().allMatch(String::isEmpty);
                if (allEmpty) {
                    continue;
                }

                // Skip header row if detected
                if (isHeaderRow(cellValues)) {
                    continue;
                }

                Question question = buildQuestionFromRow(cellValues);
                if (question != null) {
                    questions.add(question);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse Excel file", e);
            throw new BadRequestException("Failed to parse Excel file: " + e.getMessage());
        }

        if (questions.isEmpty()) {
            throw new BadRequestException("No valid questions found in the uploaded Excel file.");
        }

        return questions;
    }

    private List<Question> parseCsv(MultipartFile file) {
        List<Question> questions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setIgnoreSurroundingSpaces(true).build())) {

            for (CSVRecord record : csvParser) {
                List<String> values = new ArrayList<>();
                for (String val : record) {
                    values.add(val != null ? val.trim() : "");
                }

                if (values.stream().allMatch(String::isEmpty)) {
                    continue;
                }

                if (isHeaderRow(values)) {
                    continue;
                }

                Question question = buildQuestionFromRow(values);
                if (question != null) {
                    questions.add(question);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse CSV file", e);
            throw new BadRequestException("Failed to parse CSV file: " + e.getMessage());
        }

        if (questions.isEmpty()) {
            throw new BadRequestException("No valid questions found in the uploaded CSV file.");
        }

        return questions;
    }

    private boolean isHeaderRow(List<String> values) {
        if (values.isEmpty()) return true;
        String first = values.get(0).toLowerCase();
        String second = values.size() > 1 ? values.get(1).toLowerCase() : "";

        return first.contains("question") || first.contains("row") || first.contains("stt") ||
               first.contains("#") || second.contains("question") || second.contains("answer");
    }

    private Question buildQuestionFromRow(List<String> values) {
        // Remove trailing empty elements
        while (!values.isEmpty() && values.get(values.size() - 1).isEmpty()) {
            values.remove(values.size() - 1);
        }

        if (values.isEmpty()) return null;

        // Check if first column is just a row index (e.g., 1, 2, 3...)
        int startIndex = 0;
        if (values.get(0).matches("^\\d+$") && values.size() >= 3) {
            // First column is row counter
            startIndex = 1;
        }

        List<String> relevant = values.subList(startIndex, values.size());
        if (relevant.isEmpty()) return null;

        String questionText = relevant.get(0);
        if (questionText.isBlank()) return null;

        String optionA = "";
        String optionB = "";
        String optionC = "";
        String optionD = "";
        String rawCorrect = "";
        String explanation = "";

        if (relevant.size() >= 6) {
            // Standard Multiple Choice format:
            // Col 0: Question | Col 1: Opt A | Col 2: Opt B | Col 3: Opt C | Col 4: Opt D | Col 5: Correct Answer | Col 6 (optional): Explanation
            optionA = relevant.size() > 1 ? relevant.get(1) : "";
            optionB = relevant.size() > 2 ? relevant.get(2) : "";
            optionC = relevant.size() > 3 ? relevant.get(3) : "";
            optionD = relevant.size() > 4 ? relevant.get(4) : "";
            rawCorrect = relevant.size() > 5 ? relevant.get(5) : "A";
            explanation = relevant.size() > 6 ? relevant.get(6) : "";
        } else if (relevant.size() == 2) {
            // Flashcard / Q&A format (Question, Correct Answer) like in the screenshot
            rawCorrect = relevant.get(1);
            optionA = rawCorrect;
            optionB = "None of the above";
            optionC = "Both A and B";
            optionD = "Not applicable";
            explanation = "Correct answer: " + rawCorrect;
        } else if (relevant.size() >= 3) {
            // Question + 2 options + answer, etc.
            optionA = relevant.get(1);
            optionB = relevant.get(2);
            if (relevant.size() >= 4) {
                rawCorrect = relevant.get(3);
            } else {
                rawCorrect = "A";
            }
        }

        String normalizedCorrect = normalizeCorrectAnswer(rawCorrect, optionA, optionB, optionC, optionD);

        return Question.builder()
                .questionText(questionText)
                .optionA(optionA.isBlank() ? "Option A" : optionA)
                .optionB(optionB.isBlank() ? "Option B" : optionB)
                .optionC(optionC.isBlank() ? null : optionC)
                .optionD(optionD.isBlank() ? null : optionD)
                .correctAnswer(normalizedCorrect)
                .explanation(explanation.isBlank() ? null : explanation)
                .build();
    }

    private String normalizeCorrectAnswer(String raw, String optA, String optB, String optC, String optD) {
        if (raw == null || raw.isBlank()) return "A";
        String trimmed = raw.trim();

        // If it's already "A", "B", "C", "D"
        if (trimmed.equalsIgnoreCase("A") || trimmed.equalsIgnoreCase("Option A") || trimmed.equals("1")) {
            return "A";
        }
        if (trimmed.equalsIgnoreCase("B") || trimmed.equalsIgnoreCase("Option B") || trimmed.equals("2")) {
            return "B";
        }
        if (trimmed.equalsIgnoreCase("C") || trimmed.equalsIgnoreCase("Option C") || trimmed.equals("3")) {
            return "C";
        }
        if (trimmed.equalsIgnoreCase("D") || trimmed.equalsIgnoreCase("Option D") || trimmed.equals("4")) {
            return "D";
        }

        // Check if value matches the text of one of the options
        if (trimmed.equalsIgnoreCase(optA)) return "A";
        if (trimmed.equalsIgnoreCase(optB)) return "B";
        if (trimmed.equalsIgnoreCase(optC)) return "C";
        if (trimmed.equalsIgnoreCase(optD)) return "D";

        return "A";
    }
}
