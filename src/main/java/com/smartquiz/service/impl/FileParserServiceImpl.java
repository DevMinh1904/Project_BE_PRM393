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
import java.util.stream.Collectors;

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
        List<List<String>> rawRows = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BadRequestException("Excel workbook does not contain any sheets.");
            }

            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();
            boolean firstRowProcessed = false;

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

                // Only the very first non-empty row can be evaluated as a header
                if (!firstRowProcessed) {
                    firstRowProcessed = true;
                    if (isHeaderRow(cellValues)) {
                        continue;
                    }
                }

                rawRows.add(cellValues);
            }

        } catch (Exception e) {
            log.error("Failed to parse Excel file", e);
            throw new BadRequestException("Failed to parse Excel file: " + e.getMessage());
        }

        return processRawRows(rawRows, "Excel");
    }

    private List<Question> parseCsv(MultipartFile file) {
        List<List<String>> rawRows = new ArrayList<>();
        boolean firstRowProcessed = false;

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

                // Only the very first non-empty row can be evaluated as a header
                if (!firstRowProcessed) {
                    firstRowProcessed = true;
                    if (isHeaderRow(values)) {
                        continue;
                    }
                }

                rawRows.add(values);
            }

        } catch (Exception e) {
            log.error("Failed to parse CSV file", e);
            throw new BadRequestException("Failed to parse CSV file: " + e.getMessage());
        }

        return processRawRows(rawRows, "CSV");
    }

    private boolean isHeaderRow(List<String> values) {
        if (values.isEmpty()) return true;
        String first = values.get(0).trim().toLowerCase();
        String second = values.size() > 1 ? values.get(1).trim().toLowerCase() : "";

        return first.equals("row") || first.equals("stt") || first.equals("#") ||
               first.contains("question") || first.startsWith("a (") ||
               second.contains("question") || second.contains("answer") ||
               second.startsWith("b (");
    }

    private List<Question> processRawRows(List<List<String>> rawRows, String fileType) {
        if (rawRows.isEmpty()) {
            throw new BadRequestException("No valid question rows found in the uploaded " + fileType + " file.");
        }

        // Clean trailing empty cells
        List<List<String>> cleanedRows = new ArrayList<>();
        for (List<String> r : rawRows) {
            List<String> rowCopy = new ArrayList<>(r);
            while (!rowCopy.isEmpty() && rowCopy.get(rowCopy.size() - 1).isEmpty()) {
                rowCopy.remove(rowCopy.size() - 1);
            }
            if (!rowCopy.isEmpty()) {
                cleanedRows.add(rowCopy);
            }
        }

        if (cleanedRows.isEmpty()) {
            throw new BadRequestException("No valid data rows found in the uploaded " + fileType + " file.");
        }

        // Check if rows represent 2-column Q&A format (or 3-column with row counter in col 0)
        boolean isQaFormat = cleanedRows.stream().allMatch(row -> {
            int len = row.size();
            if (len == 2) return true;
            if (len == 3 && row.get(0).matches("^\\d+$")) return true;
            return false;
        });

        if (isQaFormat) {
            return processQaFormat(cleanedRows);
        } else {
            return processStandardMultipleChoice(cleanedRows);
        }
    }

    /**
     * Handles Q&A format where user only inputs Question and Correct Answer (Columns A & B, or Row # in Col A, Question in Col B, Answer in Col C).
     * Collects all correct answers across the file as an answer pool,
     * and for each question, randomly picks 3 distractors from the pool + 1 correct answer,
     * shuffling the 4 options across A, B, C, D.
     */
    private List<Question> processQaFormat(List<List<String>> rows) {
        record RawItem(String question, String answer) {}
        List<RawItem> rawItems = new ArrayList<>();

        for (List<String> row : rows) {
            int startIndex = 0;
            if (row.size() >= 3 && row.get(0).matches("^\\d+$")) {
                startIndex = 1;
            }
            if (row.size() > startIndex + 1) {
                String q = row.get(startIndex).trim();
                String a = row.get(startIndex + 1).trim();
                if (!q.isEmpty() && !a.isEmpty()) {
                    rawItems.add(new RawItem(q, a));
                }
            }
        }

        if (rawItems.isEmpty()) {
            throw new BadRequestException("Could not extract any Question and Answer pairs from file.");
        }

        // Distinct pool of all correct answers
        List<String> allAnswers = rawItems.stream()
                .map(RawItem::answer)
                .distinct()
                .collect(Collectors.toList());

        List<Question> questions = new ArrayList<>();
        java.security.SecureRandom random = new java.security.SecureRandom();

        for (RawItem item : rawItems) {
            String target = item.answer();

            // Candidate distractors: other distinct answers in the pool
            List<String> distractors = allAnswers.stream()
                    .filter(a -> !a.equalsIgnoreCase(target))
                    .collect(Collectors.toCollection(ArrayList::new));

            java.util.Collections.shuffle(distractors, random);

            List<String> chosenDistractors = new ArrayList<>();
            for (String d : distractors) {
                if (chosenDistractors.size() >= 3) break;
                chosenDistractors.add(d);
            }

            // Fallback distractors only if the pool has fewer than 4 total questions
            List<String> fallbacks = List.of("None of the above", "All of the above", "Not applicable");
            for (String fb : fallbacks) {
                if (chosenDistractors.size() >= 3) break;
                if (!chosenDistractors.contains(fb) && !target.equalsIgnoreCase(fb)) {
                    chosenDistractors.add(fb);
                }
            }

            // Assemble the 4 options: 1 correct answer + 3 distractors
            List<String> options = new ArrayList<>();
            options.add(target);
            options.addAll(chosenDistractors);

            // Shuffle options so the correct answer is not always Option A
            java.util.Collections.shuffle(options, random);

            // Determine which letter received the correct answer
            int correctIndex = -1;
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).equalsIgnoreCase(target)) {
                    correctIndex = i;
                    break;
                }
            }

            String correctLetter = switch (correctIndex) {
                case 0 -> "A";
                case 1 -> "B";
                case 2 -> "C";
                case 3 -> "D";
                default -> "A";
            };

            Question question = Question.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .questionText(item.question())
                    .optionA(options.size() > 0 ? options.get(0) : "Option A")
                    .optionB(options.size() > 1 ? options.get(1) : "Option B")
                    .optionC(options.size() > 2 ? options.get(2) : null)
                    .optionD(options.size() > 3 ? options.get(3) : null)
                    .correctAnswer(correctLetter)
                    .correctAnswerText(target)
                    .explanation("Correct answer: " + target)
                    .build();

            questions.add(question);
        }

        return questions;
    }

    private List<Question> processStandardMultipleChoice(List<List<String>> rows) {
        List<Question> questions = new ArrayList<>();
        for (List<String> row : rows) {
            int startIndex = 0;
            if (row.get(0).matches("^\\d+$") && row.size() >= 3) {
                startIndex = 1;
            }

            List<String> relevant = row.subList(startIndex, row.size());
            if (relevant.isEmpty()) continue;

            String questionText = relevant.get(0);
            if (questionText.isBlank()) continue;

            String optionA = relevant.size() > 1 ? relevant.get(1) : "Option A";
            String optionB = relevant.size() > 2 ? relevant.get(2) : "Option B";
            String optionC = relevant.size() > 3 ? relevant.get(3) : null;
            String optionD = relevant.size() > 4 ? relevant.get(4) : null;
            String rawCorrect = relevant.size() > 5 ? relevant.get(5) : "A";
            String explanation = relevant.size() > 6 ? relevant.get(6) : null;

            String normalizedCorrect = normalizeCorrectAnswer(rawCorrect, optionA, optionB, optionC, optionD);
            String correctText = switch (normalizedCorrect) {
                case "B" -> optionB;
                case "C" -> optionC;
                case "D" -> optionD;
                default -> optionA;
            };

            Question question = Question.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .questionText(questionText)
                    .optionA(optionA)
                    .optionB(optionB)
                    .optionC(optionC)
                    .optionD(optionD)
                    .correctAnswer(normalizedCorrect)
                    .correctAnswerText(correctText)
                    .explanation((explanation != null && !explanation.isBlank()) ? explanation : "Correct answer: " + correctText)
                    .build();

            questions.add(question);
        }

        return questions;
    }

    private String normalizeCorrectAnswer(String raw, String optA, String optB, String optC, String optD) {
        if (raw == null || raw.isBlank()) return "A";
        String trimmed = raw.trim();

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

        if (optA != null && trimmed.equalsIgnoreCase(optA)) return "A";
        if (optB != null && trimmed.equalsIgnoreCase(optB)) return "B";
        if (optC != null && trimmed.equalsIgnoreCase(optC)) return "C";
        if (optD != null && trimmed.equalsIgnoreCase(optD)) return "D";

        return "A";
    }
}
