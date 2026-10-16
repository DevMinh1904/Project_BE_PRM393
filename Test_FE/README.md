# SmartQuiz API Test Frontend

A lightweight, dedicated testing frontend to test and verify all SmartQuiz Spring Boot REST API endpoints locally.

## Features Included
1. **Global Session & Auth Status**:
   - Dynamic API base URL configuration (default: `http://localhost:8080`).
   - Auto-attaches JWT Bearer token once logged in or registered.
   - Quick one-click demo user button (`alex@smartquiz.com` / `secret123`).
2. **Authentication & Profile Endpoints**:
   - `POST /api/auth/send-otp` - Send 6-digit registration OTP to email.
   - `POST /api/auth/register` - Verify OTP and complete account registration.
   - `POST /api/auth/login` - Authenticate and obtain JWT token.
   - `GET /api/auth/profile/{userId}` - View user profile and streak count.
3. **Quizzes & Questions Endpoints**:
   - `GET /api/quizzes` - List all quizzes with optional `category`, `difficulty`, and `search` filters.
   - `GET /api/quizzes/{id}` - Fetch quiz metadata along with all questions.
   - `POST /api/quizzes/import` - Multipart upload for `.xlsx`, `.xls`, or `.csv` files (e.g. `sample_files/flutter_quiz_sample.xlsx` or `sample_files/quiz_sample.csv`).
   - `POST /api/quizzes` - Create a quiz set manually via JSON (includes sample loader).
   - `POST /api/quizzes/{id}/submit` - Submit answers, compute score percentage, and update streak.
   - `DELETE /api/quizzes/{id}` - Delete quiz set.
4. **Notifications Endpoints**:
   - `GET /api/notifications/{userId}` - View study alerts and reminders.
   - `POST /api/notifications` - Send custom notification to user.
   - `PUT /api/notifications/{id}/read` - Mark alert as read.
5. **Live Response Console**:
   - Real-time display of HTTP status, execution latency (ms), and pretty-printed JSON payloads.

---

## How to Run Locally

### Option 1: Open Directly in Browser (No Server Needed)
Because CORS is configured with `*` in Spring Boot `WebConfig.java`:
- Simply double-click [`Test_FE/index.html`](file:///d:/Project_PRM393/Test_FE/index.html) or open it with Chrome/Edge/Firefox.

### Option 2: Run with Python HTTP Server
Open PowerShell in the project root:
```powershell
cd Test_FE
python -m http.server 3000
```
Then visit: `http://localhost:3000`

### Option 3: Run with Node.js (`npx serve`)
```powershell
npx -y serve Test_FE -p 3000
```
Then visit: `http://localhost:3000`

---

## Running the Spring Boot Backend
Ensure your backend is running:
```powershell
.\mvnw.cmd spring-boot:run
```
By default, the backend will listen on `http://localhost:8088`.
