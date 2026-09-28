/* FinSight File Notes: Loads transactions and manages monthly grouping and expand/collapse behaviour. */
document.addEventListener("DOMContentLoaded", async () => {
  const input = qs("#fileInput");
  const zone = qs("#dropZone");
  const selected = qs("#selectedFile");
  const msg = qs("#uploadMessage");
  const validation = qs("#fileValidation");
  const uploadButton = qs("#uploadButton");
  let selectedValidation = null;

  function setValidation(id, state, text) {
    const row = qs(`#${id}`);
    if (!row) return;
    row.className = `validation-check ${state}`;
    const icon = row.querySelector("i");
    const strong = row.querySelector("strong");
    if (icon) icon.className = `bi ${state === "valid" ? "bi-check-circle-fill" : state === "invalid" ? "bi-x-circle-fill" : "bi-circle"}`;
    if (strong) strong.textContent = text;
  }

  function parseCsvLine(line) {
    const values = [];
    let current = "";
    let quoted = false;
    for (let i = 0; i < line.length; i++) {
      const char = line[i];
      if (char === '"') {
        if (quoted && line[i + 1] === '"') { current += '"'; i++; }
        else quoted = !quoted;
      } else if (char === "," && !quoted) {
        values.push(current.trim());
        current = "";
      } else {
        current += char;
      }
    }
    if (quoted) throw new Error("The CSV contains an unclosed quoted value.");
    values.push(current.trim());
    return values;
  }

  function normalizeHeader(value) {
    return String(value || "").trim().toLowerCase().replace(/\s+/g, "_");
  }

  function hasColumn(headers, aliases) {
    return aliases.some(alias => headers.includes(alias));
  }

  function parseDate(value) {
    const text = String(value || "").trim();
    if (!text) return null;
    const iso = text.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
    if (iso) {
      const date = new Date(Number(iso[1]), Number(iso[2]) - 1, Number(iso[3]));
      return Number.isNaN(date.getTime()) ? null : date;
    }
    const slash = text.match(/^(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{4})$/);
    if (slash) {
      const date = new Date(Number(slash[3]), Number(slash[2]) - 1, Number(slash[1]));
      return Number.isNaN(date.getTime()) ? null : date;
    }
    const parsed = new Date(text);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  async function validateFile(file) {
    selectedValidation = null;
    validation?.classList.remove("hidden");
    setValidation("formatCheck", "pending", "Checking...");
    setValidation("columnsCheck", "pending", "Checking...");
    setValidation("monthsCheck", "pending", "Checking...");
    uploadButton.disabled = true;

    if (!file) {
      setMessage(msg, "Please select a CSV file.");
      return;
    }

    if (!file.name.toLowerCase().endsWith(".csv")) {
      setValidation("formatCheck", "invalid", "Invalid — CSV only");
      setValidation("columnsCheck", "invalid", "Not checked");
      setValidation("monthsCheck", "invalid", "Not checked");
      setMessage(msg, "Invalid file format. FinSight accepts CSV transaction files only.");
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setValidation("formatCheck", "invalid", "Invalid — over 10 MB");
      setValidation("columnsCheck", "invalid", "Not checked");
      setValidation("monthsCheck", "invalid", "Not checked");
      setMessage(msg, "File is too large. The maximum size is 10 MB.");
      return;
    }

    setValidation("formatCheck", "valid", "Valid CSV");

    try {
      const text = await file.text();
      const lines = text.split(/\r?\n/).filter(line => line.trim());
      if (!lines.length) throw new Error("The CSV file is empty.");

      const headers = parseCsvLine(lines[0]).map(normalizeHeader);
      const hasDate = hasColumn(headers, ["date", "transaction_date", "transactiondate"]);
      const hasDescription = hasColumn(headers, ["description", "transaction_description", "transactiondescription", "details", "merchant", "name"]);
      const hasAmount = hasColumn(headers, ["amount", "transaction_amount", "transactionamount", "value"]);

      if (!hasDate || !hasDescription || !hasAmount) {
        setValidation("columnsCheck", "invalid", "Missing Date, Description or Amount");
        setValidation("monthsCheck", "invalid", "Cannot verify period");
        setMessage(msg, "Invalid CSV structure. Required columns are Date, Description and Amount.");
        return;
      }
      setValidation("columnsCheck", "valid", "Required columns found");

      const dateIndex = headers.findIndex(h => ["date", "transaction_date", "transactiondate"].includes(h));
      const months = new Set();
      const dates = [];
      let invalidRows = 0;
      for (let i = 1; i < lines.length; i++) {
        const values = parseCsvLine(lines[i]);
        const date = parseDate(values[dateIndex]);
        if (!date) { invalidRows++; continue; }
        dates.push(date);
        months.add(`${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`);
      }

      if (!dates.length) {
        setValidation("monthsCheck", "invalid", "No valid transaction dates");
        setMessage(msg, "The CSV does not contain valid transaction dates.");
        return;
      }

      dates.sort((a, b) => a - b);
      const first = dates[0];
      const last = dates[dates.length - 1];
      const monthSpan = (last.getFullYear() - first.getFullYear()) * 12 + last.getMonth() - first.getMonth();
      const meetsMonths = months.size >= 3 && monthSpan >= 2;

      if (!meetsMonths) {
        setValidation("monthsCheck", "invalid", `${months.size} calendar month(s) - 3+ required`);
        setMessage(msg, "The file does not meet the 3+ months requirement. Please upload transactions covering at least 3 calendar months.");
        return;
      }

      setValidation("monthsCheck", "valid", `${months.size} calendar months — requirement met`);
      selectedValidation = { valid: true, rows: lines.length - 1 - invalidRows, months: months.size };
      uploadButton.disabled = false;
      setMessage(msg, `File looks valid: ${selectedValidation.rows} transaction row(s) across ${selectedValidation.months} calendar months. You can start the analysis.`, true);
    } catch (error) {
      setValidation("columnsCheck", "invalid", "Could not read CSV");
      setValidation("monthsCheck", "invalid", "Not verified");
      setMessage(msg, error.message || "Unable to validate the CSV file.");
    }
  }

  async function selectFile(file) {
    selected.textContent = file ? file.name : "No file selected";
    await validateFile(file);
  }

  input?.addEventListener("change", () => selectFile(input.files[0]));
  ["dragenter", "dragover"].forEach(eventName => zone?.addEventListener(eventName, e => { e.preventDefault(); zone.classList.add("dragover"); }));
  ["dragleave", "drop"].forEach(eventName => zone?.addEventListener(eventName, e => { e.preventDefault(); zone.classList.remove("dragover"); }));
  zone?.addEventListener("drop", async e => {
    const file = e.dataTransfer.files[0];
    if (file) { input.files = e.dataTransfer.files; await selectFile(file); }
  });

  qs("#cancelUpload")?.addEventListener("click", () => {
    input.value = "";
    selected.textContent = "No file selected";
    selectedValidation = null;
    validation?.classList.add("hidden");
    uploadButton.disabled = false;
    setMessage(msg, "");
  });

  uploadButton?.addEventListener("click", async () => {
    const file = input.files[0];
    if (!file) { setMessage(msg, "Please select a CSV file."); return; }
    if (!selectedValidation?.valid) {
      await validateFile(file);
      if (!selectedValidation?.valid) return;
    }

    try {
      uploadButton.disabled = true;
      setMessage(msg, "Uploading your validated transaction file...");
      const result = await API.uploadTransactions(file);
      const uploadId = result.uploadId;
      if (!uploadId) throw new Error("Upload completed but no upload ID was returned.");
      localStorage.setItem("finsight_last_upload_id", String(uploadId));
      setMessage(msg, "Upload completed successfully. Opening the analysis screen...", true);
      setTimeout(() => location.href = `analysis.html?uploadHistoryId=${uploadId}`, 500);
    } catch (error) {
      uploadButton.disabled = false;
      setMessage(msg, error.message);
    }
  });

  const transactionStateKey = (() => {
    const user = currentUser();
    return `finsight_transaction_collapse_state_${user.email || user.id || "current-user"}`;
  })();

  function loadTransactionGroupState() {
    try { return JSON.parse(localStorage.getItem(transactionStateKey) || "{}"); } catch { return {}; }
  }

  function saveTransactionGroupState(state) {
    localStorage.setItem(transactionStateKey, JSON.stringify(state || {}));
  }

  function applyTransactionGroupState() {
    const state = loadTransactionGroupState();
    qsa(".transaction-group-toggle").forEach(button => {
      const key = button.dataset.monthKey || "";
      const collapsed = state[key] === true;
      button.setAttribute("aria-expanded", collapsed ? "false" : "true");
      const body = document.querySelector(`[data-group-body="${button.dataset.group}"]`);
      body?.classList.toggle("collapsed", collapsed);
      const icon = button.querySelector("i");
      if (icon) icon.className = `bi ${collapsed ? "bi-chevron-right" : "bi-chevron-down"}`;
    });
  }

  try {
    const transactions = await API.getTransactions();
    const rows = Array.isArray(transactions) ? transactions : (transactions.transactions || []);
    const groups = {};
    rows.forEach(t => { const key = String(t.transactionDate || "").slice(0, 7) || "Unknown"; (groups[key] ||= []).push(t); });
    const orderedGroups = Object.keys(groups).sort().reverse();
    const monthLabel = key => { if (key === "Unknown") return key; const [y,m] = key.split("-"); return new Date(Number(y), Number(m)-1, 1).toLocaleDateString(undefined,{month:"long",year:"numeric"}); };
    const state = loadTransactionGroupState();
    qs("#allTransactionsBody").innerHTML = orderedGroups.length ? orderedGroups.map((month, groupIndex) => {
      const collapsed = state[month] === true;
      return `
      <tr class="transaction-group-row"><td colspan="5"><button type="button" class="transaction-group-toggle" data-group="transaction-group-${groupIndex}" data-month-key="${esc(month)}" aria-expanded="${collapsed ? "false" : "true"}"><i class="bi ${collapsed ? "bi-chevron-right" : "bi-chevron-down"}"></i><strong>${esc(monthLabel(month))}</strong><span>${groups[month].length} transaction${groups[month].length===1?"":"s"}</span></button></td></tr>
      <tr class="transaction-group-body ${collapsed ? "collapsed" : ""}" data-group-body="transaction-group-${groupIndex}"><td colspan="5"><div class="table-scroll"><table class="nested-transaction-table"><tbody>${groups[month].map(t => `<tr><td>${esc(formatDate(t.transactionDate))}</td><td>${esc(t.description || "—")}</td><td><span class="category-badge">${esc(t.category || "OTHER")}</span></td><td>${money(t.amount)}</td><td>${esc(t.type || "EXPENSE")}</td></tr>`).join("")}</tbody></table></div></td></tr>`;
    }).join("") : `<tr><td colspan="5">No transactions available.</td></tr>`;
    qsa(".transaction-group-toggle").forEach(button => button.addEventListener("click", () => {
      const id = button.dataset.group;
      const monthKey = button.dataset.monthKey || "";
      const body = document.querySelector(`[data-group-body="${id}"]`);
      const expanded = button.getAttribute("aria-expanded") === "true";
      const nextCollapsed = expanded;
      button.setAttribute("aria-expanded", nextCollapsed ? "false" : "true");
      body?.classList.toggle("collapsed", nextCollapsed);
      const icon = button.querySelector("i");
      if (icon) icon.className = `bi ${nextCollapsed ? "bi-chevron-right" : "bi-chevron-down"}`;
      const nextState = loadTransactionGroupState();
      nextState[monthKey] = nextCollapsed;
      saveTransactionGroupState(nextState);
    }));
    qs("#collapseAllTransactions")?.addEventListener("click", () => {
      const nextState = loadTransactionGroupState();
      qsa(".transaction-group-toggle").forEach(button => {
        const key = button.dataset.monthKey || "";
        nextState[key] = true;
        button.setAttribute("aria-expanded", "false");
        const body = document.querySelector(`[data-group-body="${button.dataset.group}"]`);
        body?.classList.add("collapsed");
        const icon = button.querySelector("i");
        if (icon) icon.className = "bi bi-chevron-right";
      });
      saveTransactionGroupState(nextState);
    });
  } catch (error) {
    qs("#allTransactionsBody").innerHTML = `<tr><td colspan="5">${esc(error.message || "Unable to load transactions.")}</td></tr>`;
  }

  try {
    const data = await API.getUploads();
    const list = Array.isArray(data) ? data : (data.uploads || []);
    qs("#uploadHistory").innerHTML = list.slice(0, 8).map(x => `<tr><td>${esc(x.fileName)}</td><td>${esc(formatDateTime(x.uploadedAt))}</td><td>${esc(x.importedRecords ?? x.totalRecords ?? "—")}</td><td><span class="status-pill ${String(x.status || "").toLowerCase() === "completed" ? "completed" : "processing"}">${esc(x.status || "PROCESSING")}</span></td></tr>`).join("") || `<tr><td colspan="4">No upload history yet.</td></tr>`;
  } catch (error) { console.warn(error); }
});
