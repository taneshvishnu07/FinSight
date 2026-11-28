/* FinSight File Notes: Provides shared frontend helpers, page shell rendering, navigation, and utility functions. */
/**
 * Format a number as currency text, e.g. money(1250.5) -> "RM1,250.50". Defaults to
 * MYR.
 */
function money(value, currency = "MYR") {
  const symbols = { MYR: "RM", USD: "$", SGD: "S$" };
  return `${symbols[currency] || currency + " "}${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/**
 * Escape HTML special characters. ALWAYS use this when inserting server or user text
 * into innerHTML to prevent XSS.
 */
function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, char => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#039;" }[char]));
}

/**
 * Show a status/error message in a form-message element (green when success is true).
 */
function setMessage(element, message, success = false) {
  if (!element) return;
  element.textContent = message || "";
  element.className = `form-message ${success ? "success" : ""}`;
}

/**
 * The logged-in user object cached by the login page ({} if none).
 */
function currentUser() {
  try { return JSON.parse(localStorage.getItem("finsight_user") || "{}"); } catch { return {}; }
}

function saveUser(user) { localStorage.setItem("finsight_user", JSON.stringify(user || {})); }

/**
 * Clear the session and every cached analysis value from localStorage, then go to the
 * landing page.
 */
function logout() {
  clearToken();
  localStorage.removeItem("finsight_user");
  localStorage.removeItem("finsight_last_upload_id");
  localStorage.removeItem("finsight_last_analysis");
  localStorage.removeItem("finsight_last_alerts");
  localStorage.removeItem("finsight_profile_completed");
  localStorage.removeItem("finsight_profile_verified_at");
  localStorage.removeItem("finsight_analysis_running");
  localStorage.removeItem("finsight_analysis_upload_id");
  localStorage.removeItem("finsight_dismissed_alert_ids");
  localStorage.removeItem("finsight_dismissed_robot_analyses");
  localStorage.removeItem("finsight_guide_step");
  localStorage.removeItem("finsight_first_login_pending");
  location.href = "index.html";
}

/**
 * Redirect to login.html when there is no token; returns whether the user is logged
 * in.
 */
function requireAuth() {
  if (!getToken()) { location.href = "login.html"; return false; }
  return true;
}

/**
 * Shorthand for document.querySelector / querySelectorAll (qsa returns a real array).
 */
function qs(selector) { return document.querySelector(selector); }
function qsa(selector) { return [...document.querySelectorAll(selector)]; }

// (Existing note) wheel handler registered with passive:false so preventDefault()
// works.
// Prevent mouse-wheel scrolling from changing numeric form values or the
// budget-year selector. Users can still edit these fields by typing or using
// their normal controls.
document.addEventListener("wheel", event => {
  const target = event.target;
  if (target instanceof HTMLInputElement &&
      (target.type === "number" || target.classList.contains("monthly-budget-input"))) {
    event.preventDefault();
    target.blur();
    return;
  }
  if (target instanceof HTMLSelectElement && target.id === "budgetYear") {
    event.preventDefault();
  }
}, { passive: false });


/**
 * The analysis pipeline can take a while, so a "running" flag is kept in localStorage;
 * pages check it to avoid showing half-finished results. setAnalysisRunning()
 * sets/clears it (and remembers the upload id).
 */
function isAnalysisRunning() {
  return localStorage.getItem("finsight_analysis_running") === "true";
}

function setAnalysisRunning(running, uploadId = null) {
  if (running) {
    localStorage.setItem("finsight_analysis_running", "true");
    if (uploadId != null) localStorage.setItem("finsight_analysis_upload_id", String(uploadId));
  } else {
    localStorage.removeItem("finsight_analysis_running");
    localStorage.removeItem("finsight_analysis_upload_id");
  }
}

/**
 * Replace one element's content with a "still running" notice.
 */
function showAnalysisPending(elementId, message = "Analysis is still running. Results will appear after all analysis stages are completed.") {
  const el = qs(elementId);
  if (el) el.innerHTML = `<p class="muted">${esc(message)}</p>`;
}

/**
 * Hide the page content and show a full-page "results not ready" notice.
 */
function showAnalysisPendingPage(message = "Analysis is still running. Please wait until all analysis stages are completed.") {
  const main = document.querySelector(".app-main");
  if (!main) return;
  main.querySelectorAll("section, .page-title-row").forEach(el => el.classList.add("analysis-hidden"));
  let box = qs("#analysisPendingMessage");
  if (!box) {
    box = document.createElement("section");
    box.id = "analysisPendingMessage";
    box.className = "panel analysis-pending-panel";
    main.appendChild(box);
  }
  box.innerHTML = `<div class="panel-header"><div><span class="eyebrow">ANALYSIS IN PROGRESS</span><h2>Results are not ready yet</h2><p class="muted">${esc(message)}</p></div></div>`;
}

/**
 * Id of the most recent upload, cached in localStorage by the upload page.
 */
function latestUploadIdFromStorage() {
  const id = localStorage.getItem("finsight_last_upload_id");
  return id ? Number(id) : null;
}

/**
 * Get the latest upload id from the cache, or ask the backend for the newest upload
 * (and cache it). Returns null if the user has not uploaded anything yet.
 */
async function resolveLatestUploadId() {
  const cached = latestUploadIdFromStorage();
  if (cached) return cached;
  const uploads = await API.getUploads();
  const list = Array.isArray(uploads) ? uploads : (uploads.uploads || []);
  const latest = list[0];
  if (!latest?.uploadId) return null;
  localStorage.setItem("finsight_last_upload_id", String(latest.uploadId));
  return latest.uploadId;
}

/**
 * Date -> "05 Mar 2026" (or an em dash when empty).
 */
function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10);
  return date.toLocaleDateString(undefined, { day:"2-digit", month:"short", year:"numeric" });
}

/**
 * Date and time -> "05 Mar 2026, 14:30".
 */
function formatDateTime(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString(undefined, { day:"2-digit", month:"short", year:"numeric", hour:"2-digit", minute:"2-digit" });
}

/**
 * Render the shared page frame (sidebar navigation, header, logout) into #appShell.
 * Until the profile is completed, only a "Complete Profile" link is shown.
 */
function renderShell(profileCompleted = true) {
  const mount = qs("#appShell");
  if (!mount) return;
  const page = location.pathname.split("/").pop() || "dashboard.html";
  const allItems = [
    ["dashboard.html","bi-grid-1x2","Dashboard"],
    ["transactions.html","bi-upload","Transactions"],
    ["report.html","bi-bar-chart-line","Financial Health & Reports"],
    ["subscriptions.html","bi-repeat","Recurring Payments & Hidden Costs"],
    ["unusual-spending.html","bi-exclamation-triangle","Unusual Transactions"],
    ["recommendations.html","bi-lightbulb","Financial Action Plan"],
    ["alerts.html","bi-bell","Alerts"],
    ["profile.html","bi-person","Profile & Settings"]
  ];
  const items = profileCompleted ? allItems : [["profile.html","bi-person","Complete Profile"]];

  mount.innerHTML = `<aside class="sidebar"><a class="brand" href="${profileCompleted ? "dashboard.html" : "profile.html"}"><span class="brand-mark"><i class="bi bi-bar-chart-fill"></i></span>FinSight</a><nav class="sidebar-nav">${items.map(([href,icon,label])=>`<a href="${href}" class="${page===href?"active":""}"><i class="bi ${icon}"></i><span>${label}</span></a>`).join("")}</nav><div class="sidebar-bottom">${profileCompleted ? `<button id="guideButton"><i class="bi bi-question-circle"></i><span>User Guide</span></button>` : `<div class="setup-lock-note"><i class="bi bi-lock"></i><span>Complete your profile to unlock the FinSight dashboard.</span></div>`}<button id="logoutButton"><i class="bi bi-box-arrow-left"></i><span>Logout</span></button></div></aside>`;

  qs("#logoutButton")?.addEventListener("click", logout);
  qs("#guideButton")?.addEventListener("click", () => window.FinSightGuide?.open());
  if (!qs("#robotAlertMount")) { const el=document.createElement("div"); el.id="robotAlertMount"; document.body.appendChild(el); }
  if (!qs("#userGuideMount")) { const el=document.createElement("div"); el.id="userGuideMount"; document.body.appendChild(el); }
}

/**
 * Route guard run on every app page: requires login, then makes sure a financial
 * profile exists, otherwise redirects to profile.html. The result is cached for 5
 * minutes in localStorage to avoid a request on every page change (the backend still
 * enforces security on each API call).
 */
async function enforceProfileGate() {
  const page = location.pathname.split("/").pop() || "dashboard.html";
  if (!document.body.classList.contains("app-page")) return;
  if (!requireAuth()) return;

  const completed = localStorage.getItem("finsight_profile_completed") === "true";
  const verifiedAt = Number(localStorage.getItem("finsight_profile_verified_at") || 0);
  const profileCacheFresh = completed && Number.isFinite(verifiedAt)
    && (Date.now() - verifiedAt) < (5 * 60 * 1000);

  // Profile data is loaded by profile.js itself. Avoid a duplicate request here.
  if (page === "profile.html") {
    renderShell(completed);
    // profile.html is the first-time setup page, so it must never remain
    // hidden behind the route-checking overlay while loadProfile() runs.
    document.body.classList.remove("route-checking");
    return;
  }

  // Most page changes can render immediately from the cached profile state.
  // The backend still enforces authentication and ownership on every API call.
  if (profileCacheFresh) {
    renderShell(true);
    return;
  }

  document.body.classList.add("route-checking");
  try {
    await API.getProfile();
    localStorage.setItem("finsight_profile_completed", "true");
    localStorage.setItem("finsight_profile_verified_at", String(Date.now()));
    renderShell(true);
  } catch (error) {
    if ((error.status === 400 || error.status === 404) && /financial profile not found/i.test(error.message || "")) {
      localStorage.setItem("finsight_profile_completed", "false");
      localStorage.removeItem("finsight_profile_verified_at");
      location.replace("profile.html");
      return;
    }
    renderShell(completed);
    const status = qs("#analysisStatus") || qs("#uploadMessage") || qs("#keyInsights");
    if (status) setMessage(status, error.message || "Unable to verify your financial profile.");
  } finally {
    document.body.classList.remove("route-checking");
  }
}

// Default Chart.js colours to match the navy theme.
if (window.Chart) {
  Chart.defaults.color = "#0B2545";
  Chart.defaults.borderColor = "rgba(11,37,69,.22)";
  Chart.defaults.scale.grid.color = "rgba(11,37,69,.14)";
  Chart.defaults.plugins.legend.labels.color = "#0B2545";
}

document.addEventListener("DOMContentLoaded", () => {
  enforceProfileGate();

  qsa("[data-toggle-password]").forEach(button => button.addEventListener("click", () => {
    const input=qs(button.dataset.togglePassword); if(!input)return;
    input.type=input.type==="password"?"text":"password";
    button.innerHTML=`<i class="bi ${input.type==="password"?"bi-eye":"bi-eye-slash"}"></i>`;
  }));

});
