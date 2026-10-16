// SmartQuiz Test Frontend Client Logic
const state = {
  token: localStorage.getItem("smartquiz_token") || "",
  userId: localStorage.getItem("smartquiz_userId") || ""
};

// Initialize badges on load
document.addEventListener("DOMContentLoaded", () => {
  updateAuthUI();
  populateSampleQuizJson();
  if (state.userId) {
    document.getElementById("profileUserId").value = state.userId;
    document.getElementById("submitUserId").value = state.userId;
    document.getElementById("notifUserId").value = state.userId;
    document.getElementById("createNotifUserId").value = state.userId;
  }
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
    userBadge.textContent = "User #" + state.userId;
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
    document.getElementById("profileUserId").value = userId;
    document.getElementById("submitUserId").value = userId;
    document.getElementById("notifUserId").value = userId;
    document.getElementById("createNotifUserId").value = userId;
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
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
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
// TAB 2: QUIZZES APIs
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
  await makeRequest("GET", `/api/quizzes${queryStr}`);
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
  if (!confirm(`Are you sure you want to delete Quiz #${id}?`)) return;
  await makeRequest("DELETE", `/api/quizzes/${id}`);
}

async function importQuizFile() {
  const fileInput = document.getElementById("importFile");
  if (!fileInput.files || fileInput.files.length === 0) {
    alert("Please select an Excel (.xlsx/.xls) or CSV (.csv) file first.");
    return;
  }

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

  await makeRequest("POST", "/api/quizzes/import", formData, true);
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
  document.getElementById("createQuizJson").value = JSON.stringify(sample, null, 2);
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
  await makeRequest("POST", "/api/quizzes", parsed);
}

async function submitQuizAnswers() {
  const quizId = document.getElementById("submitQuizId").value.trim();
  const inputUserId = document.getElementById("submitUserId").value.trim();
  const userId = inputUserId || state.userId;

  if (!quizId) {
    alert("Please enter a Quiz ID.");
    return;
  }
  if (!userId) {
    alert("Please specify a User ID.");
    return;
  }

  const rawAnswers = document.getElementById("submitAnswersJson").value;
  let answersMap;
  try {
    answersMap = JSON.parse(rawAnswers);
  } catch (e) {
    alert("Invalid answers JSON: " + e.message);
    return;
  }

  const payload = {
    userId: userId,
    answers: answersMap
  };

  await makeRequest("POST", `/api/quizzes/${quizId}/submit`, payload);
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
