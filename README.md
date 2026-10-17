# SmartQuiz Mobile Application Backend (Spring Boot 3.x REST API)

Enterprise-grade Spring Boot 3 backend developed for the **SmartQuiz** Android mobile application (PRM393 course project).

## 🚀 Key Features
- **Spring Boot 3.3.4 & Java 21+** with **Spring Data MongoDB**.
- **MongoDB NoSQL Integration**: High-performance document database with automatic index creation and embedded document patterns.
- **Dynamic Excel & CSV Parsing (Apache POI 5.2.5 + Commons CSV)**:
  - Supports multipart file uploads up to **15MB**.
  - Parses `.xlsx`, `.xls`, and `.csv` files.
  - Automatically identifies questions, option sets (A, B, C, D), and answer keys.
  - Automatically generates intelligent distractors and randomized option slots from 2-column Q&A sheets.
  - Stores structured `QuizSet` with embedded `Question` documents in MongoDB.
- **Full Authentication**: User registration with email uniqueness validation, password verification, and streak tracking.
- **Secure Email OTP Verification**: 6-digit verification code with 5-minute expiration and automated daily database purging.
- **Quiz Taking & Grading Engine**: Automated scoring, streak calculation, average score tracking, and instant completion alert generation.
- **Push Notification Service**: User study reminders, streak badges, and quiz alerts.
- **OpenAPI 3 / Swagger UI**: Built-in interactive API playground.
- **Global CORS Configured**: Seamless integration with mobile apps and web clients (`@CrossOrigin(origins = "*")`).

---

## 🛠️ Tech Stack
- **Framework:** Spring Boot 3.3.4
- **Language:** Java 21 (LTS)
- **Database:** MongoDB (Cloud Atlas / Local)
- **Persistence:** Spring Data MongoDB
- **Spreadsheet Engine:** Apache POI 5.2.5 (`poi`, `poi-ooxml`) + Apache Commons CSV 1.10.0
- **Documentation:** Springdoc OpenAPI 2.5.0 (Swagger UI)
- **Utility:** Project Lombok

---

## ⚙️ Environment Configuration

For security best practices, sensitive credentials (database connection, email passwords, JWT secrets) are injected via environment variables.

### Environment Variables Template

| Variable | Description | Example / Default |
|---|---|---|
| `PORT` | Application server port | `8088` |
| `MONGODB_URI` | MongoDB connection URI | `mongodb://localhost:27017/smartquiz_db` |
| `JWT_SECRET` | Secret key for signing JWT tokens | *(At least 256-bit secure random string)* |
| `SPRING_MAIL_HOST` | SMTP server host | `smtp.gmail.com` |
| `SPRING_MAIL_PORT` | SMTP server port | `587` |
| `SPRING_MAIL_USERNAME` | SMTP sender email address | `your-email@example.com` |
| `SPRING_MAIL_PASSWORD` | SMTP app password | `your-app-password` |

> [!IMPORTANT]
> **Security Notice:** Never commit raw database passwords, API keys, or mail app passwords directly into version control. Always configure them as environment variables on your hosting provider (e.g. Render) or in a local `.env` file that is ignored by Git.

---

## ⚡ Running Locally

### 1. Database Setup
Ensure MongoDB is running locally on port `27017` or provide your cloud MongoDB connection URI via `MONGODB_URI`.

```bash
# Example local MongoDB URI:
export MONGODB_URI="mongodb://localhost:27017/smartquiz_db"
```

### 2. Start the Server
Run the application using the Maven wrapper:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
The server will start on port **8088** (or the port defined by `PORT`).

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
| 1 | `POST` | `/api/auth/send-otp` | Send 6-digit verification code to user email (expires in 5 mins) |
| 2 | `POST` | `/api/auth/register` | Register account with fullName, email, password, and OTP code |
| 3 | `POST` | `/api/auth/login` | Authenticate user and receive JWT Bearer token |
| - | `GET` | `/api/auth/profile/{userId}` | Fetch user profile and study streak details |

### 2. Quizzes (`/api/quizzes`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/quizzes` | Fetch all quiz sets with optional filters (`category`, `difficulty`, `search`) |
| `GET` | `/api/quizzes/{id}` | Retrieve full quiz details with questions |
| `POST` | `/api/quizzes` | Create a quiz set manually via JSON |
| `POST` | `/api/quizzes/import` | **Import quiz questions from `.xlsx`, `.xls`, or `.csv` (Multipart up to 15MB)** |
| `POST` | `/api/quizzes/{id}/submit` | Submit answers, calculate score, increment streak, generate alert |
| `DELETE` | `/api/quizzes/{id}` | Delete a quiz set and its questions |

### 3. Notifications (`/api/notifications`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/notifications/{userId}` | Fetch user notifications and study reminders |
| `POST` | `/api/notifications` | Dispatch custom notification or study alert |
| `PUT` | `/api/notifications/{id}/read` | Mark a notification as read |

---

## 📊 Excel & CSV Import Format Guide

The import endpoint accepts `.xlsx`, `.xls`, or `.csv` files formatted in either of the two supported schemas:

### Format A: Standard 4-Option Multiple Choice (Columns A-G)
| Col A (Question) | Col B (Option A) | Col C (Option B) | Col D (Option C) | Col E (Option D) | Col F (Answer) | Col G (Explanation) |
|---|---|---|---|---|---|---|
| What language is used for Flutter? | Dart | Kotlin | Java | Swift | A | Powered by Dart |
| Google's recommended state management? | Provider | Redux | MobX | Zustand | A | Officially endorsed |

### Format B: Q&A / Flashcard (Columns A-B or A-C)
| Col A (Row / Question) | Col B (Question / Answer) | Col C (Correct Answer) |
|---|---|---|
| What is the primary language for Android? | Kotlin | Officially supported by Google |
| Which widget repeats list items efficiently? | ListView.builder | Recycles views on scroll |

> **Smart Option Generation:** For 2-column Q&A files, the parser automatically pools answers across the entire file, selects 3 intelligent distractors, and randomizes all 4 options (A, B, C, D) automatically!

Sample import files are located in `sample_files/`.
