/* FinSight File Notes: Provides the shared frontend API helper used to call the Spring Boot backend and attach the user token. */
/**
 * Frontend configuration for talking to the backend. baseUrl defaults to "/api" (same
 * origin as the Spring Boot app that serves these pages); set
 * window.FINSIGHT_API_BASE_URL before this script to point at another host. The JWT
 * and the user object are kept in localStorage under these keys.
 */
const FINSIGHT_API = {
  baseUrl: window.FINSIGHT_API_BASE_URL || "/api",
  tokenKey: "finsight_token",
  userKey: "finsight_user"
};

/**
 * Token helpers: read, store and remove the JWT in localStorage.
 */
function getToken() { return localStorage.getItem(FINSIGHT_API.tokenKey); }
function setToken(token) { localStorage.setItem(FINSIGHT_API.tokenKey, token); }
function clearToken() { localStorage.removeItem(FINSIGHT_API.tokenKey); }

/**
 * Central fetch wrapper used by every API call.
 *
 * - Adds the "Authorization: Bearer <jwt>" header when logged in.
 * - Sets JSON Content-Type unless the body is FormData (file upload).
 * - Parses the response as JSON (falls back to {raw: text}).
 * - 401: clears the session and redirects to login.html.
 * - Other errors: throws an Error whose message comes from the backend's {message} body (see GlobalExceptionHandler) and whose `status` is the HTTP code.
 */
async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (!(options.body instanceof FormData) && options.body !== undefined) headers.set("Content-Type", "application/json");

  let response;
  try {
    response = await fetch(`${FINSIGHT_API.baseUrl}${path}`, { ...options, headers });
  } catch {
    throw new Error("Cannot connect to FinSight. Make sure Spring Boot is running on port 8080.");
  }

  const text = await response.text();
  let data = {};
  try { data = text ? JSON.parse(text) : {}; } catch { data = { raw: text }; }

  if (response.status === 401) {
    clearToken();
    localStorage.removeItem(FINSIGHT_API.userKey);
    if (!location.pathname.endsWith("login.html")) location.href = "login.html";
    throw new Error(data.message || "Your session has expired. Please sign in again.");
  }
  if (!response.ok) { const error = new Error(data.message || data.error || data.raw || `Request failed (${response.status})`); error.status = response.status; throw error; }
  return data;
}

/**
 * One function per backend endpoint (see the controller package). Each returns the
 * parsed JSON. Upload-related calls take the `uploadHistoryId` returned by
 * uploadTransactions().
 */
const API = {
  login: body => apiRequest("/auth/login", { method: "POST", body: JSON.stringify(body) }),
  changePassword: body => apiRequest("/auth/password", { method: "PATCH", body: JSON.stringify(body) }),
  register: body => apiRequest("/auth/register", { method: "POST", body: JSON.stringify(body) }),
  getProfile: () => apiRequest("/financial-profile/me"),
  getMonthlyBudgets: () => apiRequest("/monthly-budgets"),
  saveMonthlyBudgets: rows => apiRequest("/monthly-budgets/bulk", { method: "PUT", body: JSON.stringify(rows) }),
  createProfile: body => apiRequest("/financial-profile", { method: "POST", body: JSON.stringify(body) }),
  updateProfile: body => apiRequest("/financial-profile", { method: "PUT", body: JSON.stringify(body) }),
  getLatestAnalysis: () => apiRequest("/analysis/latest"),
  getAnalysisById: id => apiRequest(`/analysis/${id}`),
  getTransactions: () => apiRequest("/transactions"),
  getUploads: () => apiRequest("/uploads"),
  getUpload: id => apiRequest(`/uploads/${id}`),
  getUploadTransactions: id => apiRequest(`/uploads/${id}/transactions`),
  uploadTransactions: file => {
    const form = new FormData();
    form.append("file", file);
    return apiRequest("/uploads/transactions", { method: "POST", body: form });
  },
  runAnalysis: uploadHistoryId => apiRequest(`/financial-analysis/run/${uploadHistoryId}`, { method: "POST" }),
  detectSubscriptions: () => apiRequest("/subscriptions/detect"),
  detectAnomalies: uploadHistoryId => apiRequest(`/anomalies/detect/${uploadHistoryId}`, { method: "POST" }),
  getRecommendations: uploadHistoryId => apiRequest(`/recommendations/${uploadHistoryId}`),
  getAlerts: uploadHistoryId => apiRequest(`/alerts/${uploadHistoryId}`),
  markAlertRead: (uploadHistoryId, alertId) => apiRequest(`/alerts/${uploadHistoryId}/${alertId}/read`, { method: "PATCH" }),
  markAllAlertsRead: uploadHistoryId => apiRequest(`/alerts/${uploadHistoryId}/read-all`, { method: "POST" }),
  forecast: (uploadHistoryId, monthsAhead = 3) => apiRequest(`/forecast/${uploadHistoryId}?monthsAhead=${monthsAhead}`, { method: "POST" }),
  getInsights: () => apiRequest("/insights")
};
