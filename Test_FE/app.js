// SmartQuiz Test Frontend Client Logic
const state = {
  token: localStorage.getItem("smartquiz_token") || "",
  userId: localStorage.getItem("smartquiz_userId") || ""
};

// Global interactive quiz taking state
let currentQuizState = {
  quiz: null,
  questions: [],
  currentIndex: 0,
  answers: {} // questionId -> "A" | "B" | "C" | "D"
};

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", () => {
  updateAuthUI();
  populateSampleQuizJson();
  if (state.userId) {
    const profileEl = document.getElementById("profileUserId");
    if (profileEl) profileEl.value = state.userId;
    const notifEl = document.getElementById("notifUserId");
    if (notifEl) notifEl.value = state.userId;
    const createNotifEl = document.getElementById("createNotifUserId");
    if (createNotifEl) createNotifEl.value = state.userId;
  }
  // Automatically fetch quizzes on start so cards appear
  getAllQuizzes();
});

function getBaseUrl() {
  const url = document.getElementById("baseUrl").value.trim();
  return url.replace(/\/+$/, "");
}

function updateAuthUI() {
  const tokenBadge = document.getElementById("tokenStatusBadge");
  const userBadge = document.getElementById("currentUserIdBadge");

  if (state.token) {
    tokenBadge.textContent = "Token Active (" + state.token.substring(0, 10) + "...)";
    tokenBadge.className = "badge badge-success";
  } else {
    tokenBadge.textContent = "No Token";
    tokenBadge.className = "badge badge-warning";
  }

  if (state.userId) {
    userBadge.textContent = "User: " + state.userId.substring(0, 8) + "...";
    userBadge.className = "badge badge-success";
  } else {
    userBadge.textContent = "Not Set";
    userBadge.className = "badge";
  }
}

function saveSession(token, userId) {
  if (token) {
    state.token = token;
    localStorage.setItem("smartquiz_token", token);
  }
  if (userId) {
    state.userId = userId;
    localStorage.setItem("smartquiz_userId", userId);
    const profileEl = document.getElementById("profileUserId");
    if (profileEl) profileEl.value = userId;
    const notifEl = document.getElementById("notifUserId");
    if (notifEl) notifEl.value = userId;
    const createNotifEl = document.getElementById("createNotifUserId");
    if (createNotifEl) createNotifEl.value = userId;
  }
  updateAuthUI();
}

function clearToken() {
  state.token = "";
  state.userId = "";
  localStorage.removeItem("smartquiz_token");
  localStorage.removeItem("smartquiz_userId");
  updateAuthUI();
  logResponse("SESSION", "Cleared active token & user ID from browser storage.", 200, 0, { status: "Logged out" });
}

function quickFillDemoUser() {
  document.getElementById("loginEmail").value = "alex@smartquiz.com";
  document.getElementById("loginPassword").value = "secret123";
  switchTab("tab-auth");
}

function switchTab(tabId) {
  document.querySelectorAll(".tab-btn").forEach(btn => btn.classList.remove("active"));
  document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));

  if (tabId === "tab-auth") document.querySelectorAll(".tab-btn")[0].classList.add("active");
  if (tabId === "tab-quiz") document.querySelectorAll(".tab-btn")[1].classList.add("active");
  if (tabId === "tab-notif") document.querySelectorAll(".tab-btn")[2].classList.add("active");

  const activeContent = document.getElementById(tabId);
  if (activeContent) activeContent.classList.add("active");
}

function clearConsole() {
  document.getElementById("consoleBody").innerHTML = "// Console cleared.\n";
}

function logResponse(method, endpoint, status, duration, data) {
  const consoleBody = document.getElementById("consoleBody");
  const timeStr = new Date().toLocaleTimeString();
  const isOk = status >= 200 && status < 300;
  const statusClass = isOk ? "status-ok" : "status-err";

  const entry = document.createElement("div");
  entry.className = "log-entry";
  entry.innerHTML = `
    <div class="log-meta">
      [${timeStr}] <strong>${method}</strong> ${endpoint} &rarr; 
      <span class="${statusClass}">HTTP ${status}</span> (${duration}ms)
    </div>
    <div>${escapeHtml(JSON.stringify(data, null, 2))}</div>
  `;
  consoleBody.prepend(entry);
}

function escapeHtml(str) {
  if (!str) return "";
  return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

async function makeRequest(method, path, body = null, isFormData = false) {
  const url = `${getBaseUrl()}${path}`;
  const startTime = Date.now();
  const headers = {};

  if (!isFormData) {
    headers["Content-Type"] = "application/json";
  }
  if (state.token) {
    headers["Authorization"] = `Bearer ${state.token}`;
  }

  const options = { method, headers };
  if (body) {
    options.body = isFormData ? body : JSON.stringify(body);
  }

  try {
    const res = await fetch(url, options);
    const duration = Date.now() - startTime;
    let data;
    const text = await res.text();
    try {
      data = text ? JSON.parse(text) : { raw: text };
    } catch {
      data = { raw: text };
    }
    logResponse(method, path, res.status, duration, data);
    return { ok: res.ok, status: res.status, data };
  } catch (err) {
    const duration = Date.now() - startTime;
    logResponse(method, path, 0, duration, {
      error: "Network / Connection Error",
      details: err.message,
      suggestion: "Make sure your Spring Boot backend is running at " + getBaseUrl()
    });
    return { ok: false, status: 0, error: err };
  }
}

// -------------------------------------------------------------
// TAB 1: AUTHENTICATION APIs
// -------------------------------------------------------------

async function sendOtp() {
  const email = document.getElementById("otpEmail").value.trim();
  if (!email) {
    alert("Please enter an email address.");
    return;
  }
  const result = await makeRequest("POST", "/api/auth/send-otp", { email });
  if (result.ok) {
    document.getElementById("regEmail").value = email;
  }
}

async function registerUser() {
  const fullName = document.getElementById("regFullName").value.trim();
  const email = document.getElementById("regEmail").value.trim();
  const password = document.getElementById("regPassword").value;
  const otp = document.getElementById("regOtp").value.trim();

  if (!fullName || !email || !password || !otp) {
    alert("Please fill in Full Name, Email, Password, and the 6-digit OTP.");
    return;
  }

  const payload = { fullName, email, password, otp };
  const result = await makeRequest("POST", "/api/auth/register", payload);

  if (result.ok && result.data && result.data.data) {
    const authData = result.data.data;
    saveSession(authData.token, authData.id);
  }
}

async function loginUser() {
  const email = document.getElementById("loginEmail").value.trim();
  const password = document.getElementById("loginPassword").value;

  if (!email || !password) {
    alert("Please enter Email and Password.");
    return;
  }

  const result = await makeRequest("POST", "/api/auth/login", { email, password });
  if (result.ok && result.data && result.data.data) {
    const authData = result.data.data;
    saveSession(authData.token, authData.id);
  }
}

async function getUserProfile() {
  const inputUserId = document.getElementById("profileUserId").value.trim();
  const userId = inputUserId || state.userId;
  if (!userId) {
    alert("Please provide a User ID or log in first.");
    return;
  }
  await makeRequest("GET", `/api/auth/profile/${userId}`);
}

// -------------------------------------------------------------
// TAB 2: QUIZZES, VISUAL LIBRARY & INTERACTIVE PLAYER
// -------------------------------------------------------------

async function getAllQuizzes() {
  const category = document.getElementById("quizFilterCat").value.trim();
  const difficulty = document.getElementById("quizFilterDiff").value;
  const search = document.getElementById("quizFilterSearch").value.trim();

  const params = new URLSearchParams();
  if (category) params.append("category", category);
  if (difficulty) params.append("difficulty", difficulty);
  if (search) params.append("search", search);

  const queryStr = params.toString() ? `?${params.toString()}` : "";
  const result = await makeRequest("GET", `/api/quizzes${queryStr}`);

  if (result.ok && result.data && Array.isArray(result.data.data)) {
    renderQuizCards(result.data.data);
  } else {
    document.getElementById("quizCardsContainer").innerHTML = `
      <div style="color: var(--danger); font-size: 0.9rem; padding: 12px 0;">
        Failed to fetch quizzes or no quizzes found. Make sure backend is running.
      </div>
    `;
  }
}

function renderQuizCards(quizzes) {
  const container = document.getElementById("quizCardsContainer");
  if (!quizzes || quizzes.length === 0) {
    container.innerHTML = `
      <div style="color: var(--text-muted); font-size: 0.9rem; padding: 12px 0;">
        No quiz sets match your filter. You can import one using the form below!
      </div>
    `;
    return;
  }

  container.innerHTML = quizzes.map(q => {
    let diffBadgeClass = "badge";
    if (q.difficulty === "EASY") diffBadgeClass = "badge badge-success";
    else if (q.difficulty === "HARD") diffBadgeClass = "badge badge-danger";
    else diffBadgeClass = "badge badge-warning";

    return `
      <div class="quiz-item-card">
        <div class="quiz-item-info">
          <h4>${escapeHtml(q.title)}</h4>
          <div class="quiz-item-meta">
            <span class="badge badge-success">${escapeHtml(q.category || "General")}</span>
            <span class="${diffBadgeClass}">${escapeHtml(q.difficulty || "MEDIUM")}</span>
            <span>📝 ${q.totalQuestions || 0} Questions</span>
            <span>⏱️ ${q.timeLimitMinutes || 15} Mins</span>
            <span>⭐ Avg: ${q.avgScore != null ? q.avgScore : 0}%</span>
          </div>
        </div>
        <div style="display: flex; gap: 8px; flex-shrink: 0;">
          <button class="btn-success" onclick="startQuiz('${q.id}')">🚀 Take Quiz</button>
          <button class="btn-sm btn-secondary" onclick="loadQuizToInspector('${q.id}')">Inspect</button>
          <button class="btn-sm btn-danger" onclick="deleteQuizDirect('${q.id}')">🗑️</button>
        </div>
      </div>
    `;
  }).join("");
}

function loadQuizToInspector(quizId) {
  document.getElementById("detailQuizId").value = quizId;
  getQuizById();
}

async function deleteQuizDirect(id) {
  if (!confirm(`Are you sure you want to delete Quiz #${id}?`)) return;
  const result = await makeRequest("DELETE", `/api/quizzes/${id}`);
  if (result.ok) {
    getAllQuizzes();
  }
}

async function getQuizById() {
  const id = document.getElementById("detailQuizId").value.trim();
  if (!id) {
    alert("Please enter a Quiz ID.");
    return;
  }
  await makeRequest("GET", `/api/quizzes/${id}`);
}

async function deleteQuiz() {
  const id = document.getElementById("detailQuizId").value.trim();
  if (!id) {
    alert("Please enter a Quiz ID.");
    return;
  }
  await deleteQuizDirect(id);
}

// -------------------------------------------------------------
// INTERACTIVE QUIZ TAKING ENGINE (1-by-1 Flow)
// -------------------------------------------------------------

async function startQuiz(quizId) {
  // Fetch full quiz with all questions
  const res = await makeRequest("GET", `/api/quizzes/${quizId}`);
  if (!res.ok || !res.data || !res.data.data) {
    alert("Could not load quiz questions. Check console.");
    return;
  }

  const quiz = res.data.data;
  if (!quiz.questions || quiz.questions.length === 0) {
    alert("This quiz has no questions yet!");
    return;
  }

  currentQuizState = {
    quiz: quiz,
    questions: quiz.questions,
    currentIndex: 0,
    answers: {}
  };

  // Hide library, show player
  document.getElementById("quizLibrarySection").style.display = "none";
  document.getElementById("quizResultsCard").style.display = "none";
  document.getElementById("quizPlayerCard").style.display = "block";

  // Scroll smoothly to player
  document.getElementById("quizPlayerCard").scrollIntoView({ behavior: "smooth" });

  renderCurrentQuestion();
}

function renderCurrentQuestion() {
  const { quiz, questions, currentIndex, answers } = currentQuizState;
  const total = questions.length;
  const current = questions[currentIndex];

  // Header info
  document.getElementById("playerQuizTitle").textContent = quiz.title;
  document.getElementById("playerQuizDesc").textContent = quiz.description || "Answer the questions to test your knowledge.";
  document.getElementById("playerCategoryBadge").textContent = quiz.category || "General";
  document.getElementById("playerDifficultyBadge").textContent = quiz.difficulty || "MEDIUM";

  // Progress Bar
  const progressPercent = Math.round(((currentIndex + 1) / total) * 100);
  document.getElementById("playerProgressText").textContent = `Question ${currentIndex + 1} of ${total}`;
  document.getElementById("playerPercentText").textContent = `${progressPercent}%`;
  document.getElementById("playerProgressBar").style.width = `${progressPercent}%`;

  // Question Text
  document.getElementById("playerQuestionText").textContent = `${currentIndex + 1}. ${current.questionText}`;

  // Options
  const options = [
    { letter: "A", text: current.optionA },
    { letter: "B", text: current.optionB },
    { letter: "C", text: current.optionC },
    { letter: "D", text: current.optionD }
  ].filter(opt => opt.text != null && String(opt.text).trim() !== "");

  const selectedAnswer = answers[current.id] || "";
  document.getElementById("playerSelectedAnswer").textContent = selectedAnswer ? `Option ${selectedAnswer}` : "None";

  const optionsContainer = document.getElementById("playerOptionsContainer");
  optionsContainer.innerHTML = options.map(opt => {
    const isSelected = selectedAnswer === opt.letter;
    return `
      <div class="quiz-option-card ${isSelected ? 'selected' : ''}" onclick="selectQuizOption('${opt.letter}')">
        <div class="option-letter">${opt.letter}</div>
        <div style="flex:1;">${escapeHtml(opt.text)}</div>
      </div>
    `;
  }).join("");

  // Navigation Buttons
  document.getElementById("playerPrevBtn").disabled = (currentIndex === 0);

  const nextBtn = document.getElementById("playerNextBtn");
  const submitBtn = document.getElementById("playerSubmitBtn");

  if (currentIndex === total - 1) {
    nextBtn.style.display = "none";
    submitBtn.style.display = "inline-block";
  } else {
    nextBtn.style.display = "inline-block";
    submitBtn.style.display = "none";
  }
}

function selectQuizOption(letter) {
  const { questions, currentIndex } = currentQuizState;
  const current = questions[currentIndex];
  currentQuizState.answers[current.id] = letter;
  renderCurrentQuestion();
}

function nextQuestion() {
  if (currentQuizState.currentIndex < currentQuizState.questions.length - 1) {
    currentQuizState.currentIndex++;
    renderCurrentQuestion();
  }
}

function prevQuestion() {
  if (currentQuizState.currentIndex > 0) {
    currentQuizState.currentIndex--;
    renderCurrentQuestion();
  }
}

function exitQuizPlayer() {
  if (confirm("Are you sure you want to exit? Your current progress will be lost.")) {
    document.getElementById("quizPlayerCard").style.display = "none";
    document.getElementById("quizLibrarySection").style.display = "block";
  }
}

async function submitCurrentQuiz() {
  const { quiz, questions, answers } = currentQuizState;

  let activeUserId = state.userId;
  if (!activeUserId) {
    // If not logged in, prompt or fallback to demo user ID
    activeUserId = prompt("Please enter your User ID to submit this quiz (or Log In first from Tab 1):", "6ad2575f3607e867926416b0");
    if (!activeUserId) return;
  }

  const answeredCount = Object.keys(answers).length;
  if (answeredCount < questions.length) {
    if (!confirm(`You have answered ${answeredCount} of ${questions.length} questions. Are you sure you want to submit now?`)) {
      return;
    }
  }

  const payload = {
    userId: activeUserId,
    answers: answers
  };

  const res = await makeRequest("POST", `/api/quizzes/${quiz.id}/submit`, payload);
  if (!res.ok || !res.data || !res.data.data) {
    alert("Submission failed. Check API response log on the right.");
    return;
  }

  showQuizResults(res.data.data);
}

function showQuizResults(result) {
  // Hide player, show results view
  document.getElementById("quizPlayerCard").style.display = "none";
  document.getElementById("quizResultsCard").style.display = "block";
  document.getElementById("quizResultsCard").scrollIntoView({ behavior: "smooth" });

  const score = result.scorePercentage || 0;
  const circle = document.getElementById("resultsScoreCircle");
  circle.textContent = `${score}%`;
  circle.style.background = score >= 70 ? "#16a34a" : (score >= 50 ? "#ea580c" : "#dc2626");

  document.getElementById("resultsQuizTitle").textContent = result.quizTitle || currentQuizState.quiz.title;
  document.getElementById("resultsSummaryText").textContent = `You scored ${score}% (${result.correctCount || 0} out of ${result.totalQuestions || 0} correct).`;
  document.getElementById("resultsStreakText").textContent = `🔥 Current Learning Streak: ${result.updatedStreak != null ? result.updatedStreak : 1} Days`;

  // Render question-by-question breakdown
  const breakdownContainer = document.getElementById("resultsBreakdownContainer");
  if (result.questionResults && result.questionResults.length > 0) {
    breakdownContainer.innerHTML = result.questionResults.map((r, i) => {
      const isCorrect = (r.isCorrect === true) || (r.correct === true) || (
        Boolean(r.selectedAnswer && r.correctAnswer && r.selectedAnswer.toString().trim().toUpperCase() === r.correctAnswer.toString().trim().toUpperCase())
      );
      const statusIcon = isCorrect ? "✅ Correct" : "❌ Incorrect";
      const cardClass = isCorrect ? "correct" : "wrong";

      return `
        <div class="breakdown-card ${cardClass}">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 6px;">
            <strong>Question ${i + 1}: ${escapeHtml(r.questionText)}</strong>
            <span class="badge ${isCorrect ? 'badge-success' : 'badge-danger'}">${statusIcon}</span>
          </div>
          <div style="font-size: 0.85rem; color: #334155; margin-bottom: 4px;">
            <span>Your Choice: <strong>${r.selectedAnswer ? 'Option ' + r.selectedAnswer : 'Not Answered'}</strong></span>
            ${!isCorrect ? ` &bull; <span style="color:#16a34a;">Correct Answer: <strong>Option ${r.correctAnswer}</strong></span>` : ''}
          </div>
          ${r.explanation ? `<div style="font-size: 0.82rem; color: #64748b; background: #fff; padding: 6px 10px; border-radius: 4px; border: 1px solid #e2e8f0; margin-top: 4px;">💡 <em>${escapeHtml(r.explanation)}</em></div>` : ''}
        </div>
      `;
    }).join("");
  } else {
    breakdownContainer.innerHTML = "<div>No breakdown available.</div>";
  }
}

function retakeCurrentQuiz() {
  currentQuizState.currentIndex = 0;
  currentQuizState.answers = {};
  document.getElementById("quizResultsCard").style.display = "none";
  document.getElementById("quizPlayerCard").style.display = "block";
  renderCurrentQuestion();
}

function closeQuizResults() {
  document.getElementById("quizResultsCard").style.display = "none";
  document.getElementById("quizLibrarySection").style.display = "block";
  getAllQuizzes();
}

async function importQuizFile() {
  const fileInput = document.getElementById("importFile");
  if (!fileInput.files || fileInput.files.length === 0) {
    alert("Please select an Excel (.xlsx/.xls) or CSV (.csv) file first.");
    return;
  }

  const uploadBtn = document.querySelector("#tab-quiz button.btn-success");
  const origBtnText = uploadBtn ? uploadBtn.textContent : "Upload";
  if (uploadBtn) {
    uploadBtn.disabled = true;
    uploadBtn.textContent = "⏳ Uploading & Generating Options...";
  }

  try {
    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    const title = document.getElementById("importTitle").value.trim();
    const category = document.getElementById("importCategory").value.trim();
    const time = document.getElementById("importTime").value.trim();
    const diff = document.getElementById("importDifficulty").value;
    const desc = document.getElementById("importDesc").value.trim();

    if (title) formData.append("title", title);
    if (category) formData.append("category", category);
    if (time) formData.append("timeLimitMinutes", time);
    if (diff) formData.append("difficulty", diff);
    if (desc) formData.append("description", desc);

    const result = await makeRequest("POST", "/api/quizzes/import", formData, true);
    if (result.ok && result.data && result.data.data) {
      const importedQuiz = result.data.data;
      alert(`🎉 Success! Quiz "${importedQuiz.title}" was imported with ${importedQuiz.totalQuestions} questions!\n\nOptions have been dynamically generated from the pooled answers. Scroll down to take your quiz!`);
      fileInput.value = "";
      document.getElementById("importTitle").value = "";
      document.getElementById("importCategory").value = "";
      document.getElementById("importDesc").value = "";
      await getAllQuizzes();
      const libraryEl = document.getElementById("quizLibrarySection");
      if (libraryEl) libraryEl.scrollIntoView({ behavior: "smooth" });
    } else {
      const errMsg = (result.data && (result.data.message || result.data.error)) || (result.error && result.error.message) || "Import failed. Check console on the right.";
      alert("❌ Import failed: " + errMsg);
    }
  } finally {
    if (uploadBtn) {
      uploadBtn.disabled = false;
      uploadBtn.textContent = origBtnText;
    }
  }
}

function populateSampleQuizJson() {
  const sample = {
    title: "Java & Spring Boot Core Exam",
    category: "Backend Development",
    description: "Assessment on Dependency Injection, Spring MVC, and REST architecture.",
    difficulty: "MEDIUM",
    timeLimitMinutes: 10,
    questions: [
      {
        questionText: "What does the @Service annotation declare in Spring Boot?",
        optionA: "A database table entity",
        optionB: "A Spring business-layer bean component",
        optionC: "A direct HTTP servlet route",
        optionD: "A configuration bean factory",
        correctAnswer: "B",
        explanation: "@Service is a stereotype annotation denoting a business service class in the service layer."
      },
      {
        questionText: "Which HTTP status code is returned for a successful creation?",
        optionA: "200 OK",
        optionB: "201 Created",
        optionC: "204 No Content",
        optionD: "202 Accepted",
        correctAnswer: "B",
        explanation: "HTTP 201 indicates that a new resource has been created successfully."
      }
    ]
  };
  const jsonEl = document.getElementById("createQuizJson");
  if (jsonEl) jsonEl.value = JSON.stringify(sample, null, 2);
}

async function createQuizManual() {
  const rawJson = document.getElementById("createQuizJson").value;
  let parsed;
  try {
    parsed = JSON.parse(rawJson);
  } catch (e) {
    alert("Invalid JSON format in QuizSet editor: " + e.message);
    return;
  }
  const result = await makeRequest("POST", "/api/quizzes", parsed);
  if (result.ok) {
    getAllQuizzes();
  }
}

// -------------------------------------------------------------
// TAB 3: NOTIFICATIONS APIs
// -------------------------------------------------------------

async function getUserNotifications() {
  const inputUserId = document.getElementById("notifUserId").value.trim();
  const userId = inputUserId || state.userId;

  if (!userId) {
    alert("Please enter a User ID.");
    return;
  }
  await makeRequest("GET", `/api/notifications/${userId}`);
}

async function createNotification() {
  const inputUserId = document.getElementById("createNotifUserId").value.trim();
  const userId = inputUserId || state.userId;
  const title = document.getElementById("createNotifTitle").value.trim();
  const message = document.getElementById("createNotifMsg").value.trim();

  if (!userId || !title || !message) {
    alert("Please fill in User ID, Title, and Message.");
    return;
  }

  const payload = {
    userId: userId,
    title,
    message
  };

  await makeRequest("POST", "/api/notifications", payload);
}

async function markNotificationRead() {
  const notifId = document.getElementById("readNotifId").value.trim();
  if (!notifId) {
    alert("Please enter a Notification ID.");
    return;
  }
  await makeRequest("PUT", `/api/notifications/${notifId}/read`);
}
