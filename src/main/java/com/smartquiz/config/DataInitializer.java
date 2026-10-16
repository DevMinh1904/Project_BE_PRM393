package com.smartquiz.config;

import com.smartquiz.entity.Notification;
import com.smartquiz.entity.Question;
import com.smartquiz.entity.QuizSet;
import com.smartquiz.entity.User;
import com.smartquiz.repository.NotificationRepository;
import com.smartquiz.repository.QuizSetRepository;
import com.smartquiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final QuizSetRepository quizSetRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Initializing demo data in MongoDB Atlas for SmartQuiz backend...");

            // 1. Create Demo User
            User demoUser = User.builder()
                    .fullName("Alex Student")
                    .email("alex@smartquiz.com")
                    .password("secret123")
                    .currentStreak(5)
                    .isVerified(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            demoUser = userRepository.save(demoUser);

            // 2. Create Notifications for Demo User
            Notification n1 = Notification.builder()
                    .title("Daily Goal Achieved! 🎯")
                    .message("You completed your study session yesterday. Keep up the 5-day streak!")
                    .timestamp(LocalDateTime.now().minusHours(4))
                    .isRead(false)
                    .userId(demoUser.getId())
                    .build();

            Notification n2 = Notification.builder()
                    .title("New Quiz Available: Flutter Architecture")
                    .message("Test your knowledge on state management and widget lifecycles in Flutter.")
                    .timestamp(LocalDateTime.now().minusDays(1))
                    .isRead(true)
                    .userId(demoUser.getId())
                    .build();

            notificationRepository.saveAll(List.of(n1, n2));

            // 3. Create Sample Quiz Sets
            QuizSet quiz1 = QuizSet.builder()
                    .title("Mobile App Architecture (PRM393)")
                    .category("Computer Science")
                    .description("Essential concepts of modern mobile development covering Flutter, Android lifecycle, and state management.")
                    .difficulty("MEDIUM")
                    .timeLimitMinutes(15)
                    .totalQuestions(5)
                    .avgScore(82.5)
                    .createdAt(LocalDateTime.now())
                    .build();

            quiz1.addQuestion(Question.builder()
                    .id(UUID.randomUUID().toString())
                    .questionText("What programming language is used for Flutter?")
                    .optionA("Dart")
                    .optionB("Kotlin")
                    .optionC("Java")
                    .optionD("Swift")
                    .correctAnswer("A")
                    .explanation("Flutter applications are written using Google's Dart programming language.")
                    .build());

            quiz1.addQuestion(Question.builder()
                    .id(UUID.randomUUID().toString())
                    .questionText("What state management package is recommended by Google for Flutter?")
                    .optionA("Redux")
                    .optionB("Provider")
                    .optionC("MobX")
                    .optionD("Zustand")
                    .correctAnswer("B")
                    .explanation("Provider is officially recommended by Google for standard Flutter state management.")
                    .build());

            quiz1.addQuestion(Question.builder()
                    .id(UUID.randomUUID().toString())
                    .questionText("Which database is commonly built into mobile platforms for local structured storage?")
                    .optionA("MongoDB")
                    .optionB("Redis")
                    .optionC("SQLite")
                    .optionD("Cassandra")
                    .correctAnswer("C")
                    .explanation("SQLite is lightweight and natively embedded in both Android and iOS.")
                    .build());

            quiz1.addQuestion(Question.builder()
                    .id(UUID.randomUUID().toString())
                    .questionText("What widget is used for repeating list items efficiently with recycling in Flutter?")
                    .optionA("Column")
                    .optionB("ListView.builder")
                    .optionC("SingleChildScrollView")
                    .optionD("Wrap")
                    .correctAnswer("B")
                    .explanation("ListView.builder constructs items lazily on demand as they scroll into view.")
                    .build());

            quiz1.addQuestion(Question.builder()
                    .id(UUID.randomUUID().toString())
                    .questionText("What format are vector graphics usually saved as in Flutter?")
                    .optionA("SVG")
                    .optionB("PNG")
                    .optionC("JPEG")
                    .optionD("BMP")
                    .correctAnswer("A")
                    .explanation("SVG stands for Scalable Vector Graphics, providing resolution-independent images.")
                    .build());

            QuizSet quiz2 = QuizSet.builder()
                    .title("Project Management Fundamentals")
                    .category("Project Management")
                    .description("Core Agile, Scrum rituals, sprint planning, and estimation techniques.")
                    .difficulty("EASY")
                    .timeLimitMinutes(10)
                    .totalQuestions(3)
                    .avgScore(90.0)
                    .createdAt(LocalDateTime.now())
                    .build();

            quiz2.addQuestion(Question.builder()
                    .id(UUID.randomUUID().toString())
                    .questionText("In Scrum, who is primarily responsible for maximizing the value of the product?")
                    .optionA("Scrum Master")
                    .optionB("Product Owner")
                    .optionC("Lead Developer")
                    .optionD("Project Sponsor")
                    .correctAnswer("B")
                    .explanation("The Product Owner is responsible for the Product Backlog and maximizing product value.")
                    .build());

            quiz2.addQuestion(Question.builder()
                    .id(UUID.randomUUID().toString())
                    .questionText("What is the timeboxed duration of a Daily Scrum meeting?")
                    .optionA("15 minutes")
                    .optionB("30 minutes")
                    .optionC("45 minutes")
                    .optionD("60 minutes")
                    .correctAnswer("A")
                    .explanation("The Daily Scrum is strictly a 15-minute timeboxed event for the Developers.")
                    .build());

            quiz2.addQuestion(Question.builder()
                    .id(UUID.randomUUID().toString())
                    .questionText("Which chart is traditionally used to show remaining work in a sprint?")
                    .optionA("Gantt Chart")
                    .optionB("Sprint Burndown Chart")
                    .optionC("Pareto Chart")
                    .optionD("Fishbone Diagram")
                    .correctAnswer("B")
                    .explanation("A Burndown chart visually demonstrates work remaining against time in a sprint.")
                    .build());

            quizSetRepository.saveAll(List.of(quiz1, quiz2));
            log.info("Demo data initialized successfully in MongoDB! 2 QuizSets, 8 Questions, and 1 User seeded.");
        }
    }
}
