package com.smartquiz;

import com.smartquiz.dto.AuthResponse;
import com.smartquiz.dto.LoginRequest;
import com.smartquiz.dto.RegisterRequest;
import com.smartquiz.dto.UserResponse;
import com.smartquiz.service.AuthService;
import com.smartquiz.service.QuizService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SmartQuizApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private com.smartquiz.repository.UserRepository userRepository;

    @Test
    void contextLoads() {
        assertNotNull(authService);
        assertNotNull(quizService);
    }

    @Test
    void testExcelImportAndCreateSampleFile() throws Exception {
        // Generate an Excel file programmatically with Apache POI
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Quiz");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Question");
        header.createCell(1).setCellValue("Option A");
        header.createCell(2).setCellValue("Option B");
        header.createCell(3).setCellValue("Option C");
        header.createCell(4).setCellValue("Option D");
        header.createCell(5).setCellValue("Correct Answer");
        header.createCell(6).setCellValue("Explanation");

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("What programming language is used for Flutter?");
        r1.createCell(1).setCellValue("Dart");
        r1.createCell(2).setCellValue("Java");
        r1.createCell(3).setCellValue("Kotlin");
        r1.createCell(4).setCellValue("Swift");
        r1.createCell(5).setCellValue("A");
        r1.createCell(6).setCellValue("Flutter is powered by Dart.");

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("What state management package is recommended by Google?");
        r2.createCell(1).setCellValue("Provider");
        r2.createCell(2).setCellValue("Redux");
        r2.createCell(3).setCellValue("MobX");
        r2.createCell(4).setCellValue("Zustand");
        r2.createCell(5).setCellValue("A");
        r2.createCell(6).setCellValue("Provider is the officially recommended solution.");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);

        // Also save to sample_files/flutter_quiz_sample.xlsx for easy manual testing on Swagger UI
        File sampleDir = new File("sample_files");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(new File(sampleDir, "flutter_quiz_sample.xlsx"))) {
            workbook.write(fos);
        }
        workbook.close();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "flutter_quiz_sample.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray()
        );

        var importedQuiz = quizService.importQuizFromFile(
                mockFile,
                "Flutter Quiz Imported",
                "Mobile Development",
                "Imported test quiz",
                20,
                "EASY"
        );

        assertNotNull(importedQuiz);
        assertEquals(2, importedQuiz.getTotalQuestions());
        assertEquals("Flutter Quiz Imported", importedQuiz.getTitle());
    }

    @Autowired
    private com.smartquiz.repository.OtpVerificationRepository otpVerificationRepository;

    @Test
    void testAuthFlow() {
        String email = "testuser_" + System.currentTimeMillis() + "@smartquiz.com";

        // 1. Send OTP to email
        authService.sendOtp(com.smartquiz.dto.SendOtpRequest.builder().email(email).build());

        // 2. Retrieve generated OTP
        var otpEntry = otpVerificationRepository.findByEmail(email).orElseThrow();
        assertNotNull(otpEntry.getOtpCode());

        // 3. Register with invalid OTP should fail
        RegisterRequest badRegisterReq = RegisterRequest.builder()
                .fullName("Test Developer")
                .email(email)
                .password("password123")
                .otp("000000")
                .build();
        assertThrows(Exception.class, () -> authService.register(badRegisterReq));

        // 4. Register with valid OTP should succeed
        RegisterRequest goodRegisterReq = RegisterRequest.builder()
                .fullName("Test Developer")
                .email(email)
                .password("password123")
                .otp(otpEntry.getOtpCode())
                .build();
        AuthResponse user = authService.register(goodRegisterReq);
        assertNotNull(user.getId());
        assertEquals(email, user.getEmail());
        assertTrue(user.getIsVerified());
        assertNotNull(user.getToken());

        // 5. Login succeeds with JWT token
        LoginRequest loginReq = LoginRequest.builder()
                .email(email)
                .password("password123")
                .build();
        AuthResponse loggedIn = authService.login(loginReq);
        assertEquals(user.getId(), loggedIn.getId());
        assertNotNull(loggedIn.getToken());
        assertEquals("Bearer", loggedIn.getTokenType());
    }
}
