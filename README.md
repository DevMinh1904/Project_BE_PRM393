# SmartQuiz Mobile Application Backend (Spring Boot 3.x REST API)

Enterprise-grade Spring Boot 3 backend developed for the **SmartQuiz** Android mobile application (PRM393 course project).

## 🚀 Key Features
- **Spring Boot 3.3.4 & Java 21+** with Spring Data JPA & Hibernate 6.
- **MySQL Database Integration** with automatic schema updates (`ddl-auto=update`).
- **Dynamic Excel & CSV Parsing (Apache POI 5.2.5 + Commons CSV)**:
  - Supports multipart file uploads up to **15MB**.
  - Parses `.xlsx`, `.xls`, and `.csv` files.
  - Automatically identifies headers, questions, option sets (A, B, C, D), and answer keys.
  - Generates structured `QuizSet` and cascaded `Question` entities in MySQL.
- **Full Authentication**: User registration with email uniqueness validation, password verification, and streak tracking.
- **Quiz Taking & Grading Engine**: Automated scoring, streak calculation, average score tracking, and instant completion alert generation.
- **Push Notification Service**: User study reminders, streak badges, and quiz alerts.
- **OpenAPI 3 / Swagger UI**: Built-in interactive API playground.
- **Global CORS Configured**: Seamless integration with mobile apps and web testing tools (`@CrossOrigin(origins = "*")`).

---

## 🛠️ Tech Stack
- **Framework:** Spring Boot 3.3.4
- **Language:** Java 21 (LTS)
- **Database:** MySQL 9.4 (Port 3306)
- **Persistence:** Spring Data JPA / Hibernate 6
- **Spreadsheet Engine:** Apache POI 5.2.5 (`poi`, `poi-ooxml`) + Apache Commons CSV 1.10.0
- **Documentation:** Springdoc OpenAPI 2.5.0 (Swagger UI)
- **Utility:** Project Lombok

---

## ⚡ Running Locally

### 1. Database Setup
Ensure MySQL is running on `localhost:3306`. The database `smartquiz_db` will be created automatically if it doesn't already exist.
Credentials in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartquiz_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=12345
```

### 2. Start the Server
Run the application using the Maven wrapper:
```bash
./mvnw spring-boot:run
```
The server will start on port **8088**.

### 3. Open Swagger UI Playground
Navigate to:
👉 **[http://localhost:8088/swagger-ui/index.html](http://localhost:8088/swagger-ui/index.html)**

Raw OpenAPI 3 JSON documentation:
👉 **[http://localhost:8088/v3/api-docs](http://localhost:8088/v3/api-docs)**

---

## 📖 REST API Endpoints Overview

### 1. Authentication (`/api/auth`)
| Step | Method | Endpoint | Description |
|---|---|---|---|
| 1 | `POST` | `/api/auth/send-otp` | Enter email -> generates 6-digit OTP and sends to email |
| 2 | `POST` | `/api/auth/register` | Enter `fullName`, `email`, `password`, and `otp` -> creates account & returns JWT Bearer token |
| 3 | `POST` | `/api/auth/login` | Enter `email` & `password` -> verifies credentials and returns user profile & JWT Bearer token |
| - | `GET` | `/api/auth/profile/{userId}` | Fetch user profile and streak details |

### 2. Quizzes (`/api/quizzes`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/quizzes` | Fetch all quiz sets with optional filter (`category`, `difficulty`, `search`) |
| `GET` | `/api/quizzes/{id}` | Retrieve quiz details including all multiple choice questions |
| `POST` | `/api/quizzes` | Create a quiz set manually via JSON |
| `POST` | `/api/quizzes/import` | **Import quiz questions from `.xlsx` or `.csv` (Multipart up to 15MB)** |
| `POST` | `/api/quizzes/{id}/submit` | Submit answers, calculate score %, update user streak, create alert |
| `DELETE` | `/api/quizzes/{id}` | Delete a quiz set and its questions |

### 3. Notifications (`/api/notifications`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/notifications/{userId}` | Fetch study reminders and alerts for user |
| `POST` | `/api/notifications` | Dispatch custom study reminder or alert |
| `PUT` | `/api/notifications/{id}/read` | Mark notification as read |

---

## 📊 Excel & CSV Import Format Guide

The import endpoint accepts `.xlsx` or `.csv` formatted in either:

### Format A: Standard 4-Option Multiple Choice (Columns A-G)
| Col A (Question) | Col B (Option A) | Col C (Option B) | Col D (Option C) | Col E (Option D) | Col F (Answer) | Col G (Explanation) |
|---|---|---|---|---|---|---|
| What language is used for Flutter? | Dart | Kotlin | Java | Swift | A | Powered by Dart |
| Google's recommended state management? | Provider | Redux | MobX | Zustand | A | Officially endorsed |

### Format B: Q&A / Flashcard (Columns A-C)
| Col A (Row #) | Col B (Question) | Col C (Correct Answer) |
|---|---|---|
| 1 | Which database is built into Flutter? | SQLite |
| 2 | What widget repeats list items efficiently? | ListView.builder |
| 3 | What format are vector graphics in Flutter? | SVG |

Sample test files are pre-generated in the repository at:
- `sample_files/flutter_quiz_sample.xlsx`
- `sample_files/quiz_sample.csv`
