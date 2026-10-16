# 🎓 SmartQuiz Backend — Team Demo & Technical Rundown

A comprehensive guide for tomorrow's team presentation, covering system architecture, live database access, endpoint walkthroughs, and step-by-step demo instructions.

---

## 📌 1. System Overview & Tech Stack

| Component | Technology | Description |
|---|---|---|
| **Backend Framework** | Spring Boot 3.3.4 (Java 21 LTS) | Modern RESTful API service with layered architecture |
| **Database** | MongoDB Atlas (Cloud NoSQL) | Cloud-hosted document database in Hong Kong (`ap-east-1`) |
| **GUI Tool** | MongoDB Compass | Official graphical database management tool |
| **File Parser** | Apache POI 5.2.5 + Commons CSV | High-speed import of `.xlsx`, `.xls`, and `.csv` files |
| **Authentication** | JWT (jjwt 0.12.5) + OTP | Stateless Bearer token security + 6-digit email OTP |
| **Cloud Hosting** | Render (Docker) | Production cloud container deployment |
| **Health Monitoring** | UptimeRobot | Automated HTTP ping every 5 minutes to prevent sleep |
| **Testing Client** | `Test_FE` (HTML5 / Vanilla JS) | Dedicated browser console to demonstrate all APIs live |
| **API Documentation**| OpenAPI 3 / Swagger UI | Built-in interactive API documentation |

---

## 🗄️ 2. How to Access the Database (MongoDB Compass)

Share this with your teammates so they can connect directly to the database:

### Connection Details
- **Tool:** [MongoDB Compass (Download)](https://www.mongodb.com/products/tools/compass)
- **Connection URI:**
  ```text
  mongodb+srv://devminhprogram_db_user:YklK8wNMj9dLow2O@smartquizdb.4exuwfu.mongodb.net/
  ```
- **Database Name:** `smartquiz_db`

### Quick Steps to Connect:
1. Open **MongoDB Compass**.
2. Click **`+ Add new connection`**.
3. Paste the connection URI above and click **Connect**.
4. In the left panel, click into **`smartquiz_db`**.

---

## 📂 3. Database Collections & Data Models

| Collection | Purpose | Key Fields | NoSQL Design Highlight |
|---|---|---|---|
| **`users`** | Student/user accounts | `fullName`, `email`, `password`, `currentStreak`, `isVerified` | Unique email index, password write-only |
| **`quiz_sets`** | Quiz libraries & questions | `title`, `category`, `difficulty`, `timeLimitMinutes`, `avgScore`, `questions: [...]` | **Embedded document pattern**: Questions live directly inside the quiz set document — zero SQL joins, ultra-fast sub-millisecond retrieval! |
| **`notifications`** | Study reminders & alerts | `userId`, `title`, `message`, `isRead`, `timestamp` | Linked by `userId` to alert users on streak & milestones |
| **`quiz_submissions`** | Quiz grading history | `userId`, `quizSetId`, `score`, `correctCount`, `totalQuestions` | Audit trail of quiz attempts |
| **`otp_verifications`**| 6-digit registration codes | `email`, `otpCode`, `expiryTime` | Auto-purged after 5 minutes and cleaned daily at midnight |

---

## 🚀 4. Step-by-Step Live Demo Script (For Tomorrow)

Follow this structured flow during your demo to impress your team:

### Phase 1: Show the Database in MongoDB Compass (1 min)
1. Open **MongoDB Compass** connected to `smartquiz_db`.
2. Show the collections: `users` and `quiz_sets`.
3. Open a document in `quiz_sets` and expand the `questions` array:
   - *Key Talking Point:* "We use MongoDB's embedded document design. Each quiz set contains all its questions inside a single document, making quiz retrieval instant and eliminating slow database joins."

---

### Phase 2: Demonstrate the Test Frontend (1 min)
1. Open `Test_FE/index.html` in your browser.
2. Highlight the **Global Status Bar** at the top:
   - API Base URL: `http://localhost:8088` (or cloud `https://project-prm393.onrender.com`)
   - Auth Token status indicator.
   - Real-time response console on the right side.

---

### Phase 3: Authentication & Security Flow (2 mins)
1. **Send OTP (`POST /api/auth/send-otp`):**
   - Enter an email (e.g. `student@test.com`) and click **Send OTP Email**.
   - Show the HTTP 200 response in the live console.
2. **Register with OTP (`POST /api/auth/register`):**
   - In MongoDB Compass, open `otp_verifications` to show the generated 6-digit OTP code.
   - Enter the name, email, password, and OTP in the frontend and click **Register & Verify**.
   - Show that a JWT Bearer token was issued and the user was created in MongoDB!
3. **Login (`POST /api/auth/login`):**
   - Click **⚡ Fill Alex Demo User** (`alex@smartquiz.com` / `secret123`) and click **Log In**.
   - Show that the token updates to **Token Active** and the User ID badge populates.
4. **Get Profile (`GET /api/auth/profile/{userId}`):**
   - Click **Fetch Profile** to show the user's current streak (e.g., 5 days) and verification status.

---

### Phase 4: Quizzes & Bulk Excel/CSV Import (2 mins)
1. Switch to the **2. Quizzes & Import** tab.
2. **Fetch Quizzes (`GET /api/quizzes`):**
   - Click **Fetch Quizzes** to display all available quizzes.
   - Test the filters (e.g. Difficulty = `MEDIUM`, Search = `Flutter`).
3. **Import Quiz from Excel (`POST /api/quizzes/import`):**
   - Under *Import Quiz*, click **Choose File** and select `sample_files/flutter_quiz_sample.xlsx` (or `sample_files/quiz_sample.csv`).
   - Click **📤 Upload & Import**.
   - Show the response: Apache POI parsed the spreadsheet and created a complete QuizSet with all questions in MongoDB.
   - Switch back to **MongoDB Compass**, click the refresh icon, and show your team the new quiz document!

---

### Phase 5: Take a Quiz & Instant Grading Engine (2 mins)
1. Under **5. Submit Quiz Answers**:
   - The Quiz ID and User ID are auto-filled.
   - Click **Submit & Compute Result**.
2. Point out the detailed evaluation in the response:
   - `scorePercentage`: Automated calculation.
   - `updatedStreak`: Streak incremented by +1.
   - `questionResults`: Breakdown of each question with `isCorrect`, user's choice, correct answer, and explanation.

---

### Phase 6: Push Notifications & Automated Scheduler (1 min)
1. Switch to the **3. Notifications** tab.
2. Click **Get Notifications**:
   - Show the newly generated notification: *"Quiz Completed: You scored 100%... Your streak is now 6 days 🔥"*.
3. Click **Mark As Read** (`PUT /api/notifications/{id}/read`):
   - Shows status changed to `isRead: true`.
4. *Talking Point:* Explain the **`OtpCleanupScheduler`**:
   - Automatically runs every night at midnight (`cron = "0 0 0 * * ?"`) to purge stale OTP records.

---

### Phase 7: Cloud Deployment & 24/7 Uptime (1 min)
1. Show the live cloud deployment on Render:
   - Cloud URL: **`https://project-prm393.onrender.com`**
   - Interactive Swagger Docs: **`https://project-prm393.onrender.com/swagger-ui/index.html`**
2. Explain **UptimeRobot**:
   - Ping monitor set to ping `/api/quizzes` every 5 minutes.
   - Keeps Render's free container permanently warm and eliminates cold starts.

---

## 🔑 5. Credentials & URLs Cheat Sheet

| Resource | Value |
|---|---|
| **MongoDB Atlas URI** | `mongodb+srv://devminhprogram_db_user:YklK8wNMj9dLow2O@smartquizdb.4exuwfu.mongodb.net/` |
| **Demo User Email** | `alex@smartquiz.com` |
| **Demo User Password** | `secret123` |
| **Local Backend Port** | `http://localhost:8088` |
| **Local Test Frontend**| `d:\Project_PRM393\Test_FE\index.html` |
| **Cloud Service URL** | `https://project-prm393.onrender.com` |
| **Swagger UI Playground** | `https://project-prm393.onrender.com/swagger-ui/index.html` |
| **Sample Excel Import**| `sample_files/flutter_quiz_sample.xlsx` |
| **Sample CSV Import** | `sample_files/quiz_sample.csv` |
