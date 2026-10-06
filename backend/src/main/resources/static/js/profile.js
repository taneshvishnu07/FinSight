/* FinSight File Notes: Loads and saves the financial profile and month-specific budgets and handles occupation-specific form fields. */
document.addEventListener("DOMContentLoaded", async () => {
  const form = qs("#profileForm");
  if (!form) return;

  let profileExists = false;
  const occupation = qs("#occupationType");
  const studentFields = qs("#studentFields");
  const employeeFields = qs("#employeeFields");
  const budgetFields = qs("#monthlyBudgetFields");
  const budgetYear = qs("#budgetYear");
  const budgetMessage = qs("#budgetMessage");
  const submitButton = qs("#profileSubmitButton");
  const months = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];

  const studentRequired = ["#institution", "#course", "#studyYear", "#monthlyAllowance"];
  const employeeRequired = ["#occupation", "#companyName", "#monthlySalary"];

  function setRequired(selectors, required) {
    selectors.forEach(selector => {
      const element = qs(selector);
      if (element) element.required = required;
    });
  }

  function updateOccupationFields() {
    const type = occupation?.value || "";
    const student = type === "STUDENT";
    const employee = type === "EMPLOYEE";
    const other = type === "OTHER";

    studentFields?.classList.toggle("hidden", !student);
    employeeFields?.classList.toggle("hidden", !(employee || other));

    const title = qs("#employmentDetailsTitle");
    if (title) title.textContent = other ? "Other Occupation Details" : "Employee Details";

    qs("#companyNameWrap")?.classList.toggle("hidden", !employee);
    qs("#monthlySalaryWrap")?.classList.toggle("hidden", !employee);
    qs("#sideIncomeWrap")?.classList.toggle("hidden", !employee);

    setRequired(studentRequired, student);
    setRequired(employeeRequired, employee);
  }

  function populateBudgetYears() {
    if (!budgetYear || budgetYear.options.length) return;
    const currentYear = new Date().getFullYear();
    for (let year = currentYear; year <= currentYear + 2; year++) {
      const option = document.createElement("option");
      option.value = String(year);
      option.textContent = String(year);
      budgetYear.appendChild(option);
    }
    budgetYear.value = String(currentYear);
  }

  function attachWholeNumberBehavior(input) {
    input.addEventListener("input", () => {
      input.value = input.value.replace(/\D+/g, "");
    });
    input.addEventListener("wheel", event => event.preventDefault(), { passive: false });
  }

  function renderBudgetFields(existingBudgets = []) {
    if (!budgetFields || !budgetYear) return;
    const year = Number(budgetYear.value);
    const byMonth = new Map(
      (Array.isArray(existingBudgets) ? existingBudgets : [])
        .filter(row => row?.budgetMonth)
        .map(row => [row.budgetMonth, row])
    );

    budgetFields.innerHTML = months.map((month, index) => {
      const monthNumber = String(index + 1).padStart(2, "0");
      const monthKey = `${year}-${monthNumber}`;
      const row = byMonth.get(monthKey);
      const value = row ? Math.trunc(Number(row.amount)) : "";
      return `
        <label class="monthly-budget-item">
          <span>${month} ${year}</span>
          <input class="monthly-budget-input" data-month="${monthKey}" type="text" inputmode="numeric" autocomplete="off" placeholder="RM 0" value="${value}">
        </label>`;
    }).join("");

    budgetFields.querySelectorAll(".monthly-budget-input").forEach(attachWholeNumberBehavior);
  }

  function collectMonthlyBudgetRows() {
    if (!budgetFields) return [];
    return [...budgetFields.querySelectorAll(".monthly-budget-input")]
      .filter(input => input.value.trim() !== "")
      .map(input => ({
        budgetMonth: input.dataset.month,
        amount: Number(input.value.trim())
      }));
  }

  function validateMonthlyBudgetRows(rows) {
    if (rows.some(row => !Number.isInteger(row.amount) || row.amount < 0)) {
      setMessage(budgetMessage, "Monthly budgets must be whole numbers equal to or greater than zero.");
      return false;
    }
    return true;
  }

  async function loadExistingBudgets() {
    if (!profileExists) return [];
    try {
      const response = await API.getMonthlyBudgets();
      return Array.isArray(response) ? response : [];
    } catch (error) {
      setMessage(budgetMessage, error.message || "Unable to load saved monthly budgets.");
      return [];
    }
  }

  async function loadProfile() {
    try {
      const data = await API.getProfile();
      profileExists = true;
      localStorage.setItem("finsight_profile_completed", "true");
      localStorage.setItem("finsight_profile_verified_at", String(Date.now()));
      qs("#passwordPanel")?.classList.remove("hidden");
      qs(".page-title-row .eyebrow") && (qs(".page-title-row .eyebrow").textContent = "PROFILE & SETTINGS");
      qs(".page-title-row h1") && (qs(".page-title-row h1").textContent = "Financial Profile & Settings");
      qs(".page-title-row p") && (qs(".page-title-row p").textContent = "Update your financial profile and monthly budgets whenever needed.");
      if (submitButton) submitButton.innerHTML = 'Save Changes <i class="bi bi-check2"></i>';

      qs("#financialGoalMonths").value = data.financialGoalMonths ?? 6;
      qs("#occupationType").value = data.occupationType ?? "";
      qs("#institution").value = data.institution ?? "";
      qs("#course").value = data.course ?? "";
      qs("#studyYear").value = data.studyYear ?? "";
      qs("#monthlyAllowance").value = data.monthlyAllowance ?? "";
      qs("#receivesScholarship").checked = !!data.receivesScholarship;
      qs("#occupation").value = data.occupation ?? "";
      qs("#companyName").value = data.companyName ?? "";
      qs("#monthlySalary").value = data.monthlySalary ?? "";
      qs("#hasSideIncome").checked = !!data.hasSideIncome;

      const budgets = Array.isArray(data.monthlyBudgets) ? data.monthlyBudgets : await loadExistingBudgets();
      renderBudgetFields(budgets);
    } catch (error) {
      profileExists = false;
      localStorage.setItem("finsight_profile_completed", "false");
      localStorage.removeItem("finsight_profile_verified_at");
      qs(".page-title-row .eyebrow") && (qs(".page-title-row .eyebrow").textContent = "GET STARTED");
      qs(".page-title-row h1") && (qs(".page-title-row h1").textContent = "Setting Up Financial Profile");
      qs(".page-title-row p") && (qs(".page-title-row p").textContent = "Complete your personal information and monthly budgets to unlock the FinSight dashboard.");
      document.title = "FinSight | Setting Up Financial Profile";
      renderBudgetFields([]);
    }
  }

  occupation?.addEventListener("change", updateOccupationFields);
  populateBudgetYears();
  renderBudgetFields([]);
  budgetYear?.addEventListener("change", async () => {
    const budgets = await loadExistingBudgets();
    renderBudgetFields(budgets);
  });

  await loadProfile();
  updateOccupationFields();

  form.addEventListener("submit", async event => {
    event.preventDefault();
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }

    const type = occupation.value;
    const monthlyBudgets = collectMonthlyBudgetRows();
    if (!validateMonthlyBudgetRows(monthlyBudgets)) return;

    const body = {
      occupationType: type,
      financialGoalMonths: Number(qs("#financialGoalMonths").value),
      monthlyBudgets
    };

    if (type === "STUDENT") {
      body.institution = qs("#institution").value.trim();
      body.course = qs("#course").value.trim();
      body.studyYear = Number(qs("#studyYear").value);
      body.monthlyAllowance = Number(qs("#monthlyAllowance").value);
      body.receivesScholarship = qs("#receivesScholarship").checked;
    } else if (type === "EMPLOYEE") {
      body.occupation = qs("#occupation").value.trim();
      body.companyName = qs("#companyName").value.trim();
      body.monthlySalary = Number(qs("#monthlySalary").value);
      body.hasSideIncome = qs("#hasSideIncome").checked;
    } else {
      body.occupation = qs("#occupation").value.trim();
    }

    const message = qs("#formMessage");
    try {
      if (submitButton) submitButton.disabled = true;
      setMessage(message, profileExists ? "Saving your profile and monthly budgets..." : "Saving your profile and monthly budgets...");

      const creatingProfile = !profileExists;
      if (creatingProfile) {
        await API.createProfile(body);
        profileExists = true;
        // Tell the dashboard to show the built-in user-guide announcement
        // once after first-time setup is completed.
        localStorage.setItem("finsight_show_user_guide_announcement", "true");
      } else {
        await API.updateProfile(body);
      }

      localStorage.setItem("finsight_profile_completed", "true");
      localStorage.setItem("finsight_profile_verified_at", String(Date.now()));

      if (creatingProfile) {
        setMessage(message, "Profile and monthly budgets saved successfully. Opening your dashboard...", true);
        setTimeout(() => {
          location.href = "dashboard.html";
        }, 650);
      } else {
        setMessage(message, "Profile and monthly budgets saved successfully.", true);
      }
    } catch (error) {
      setMessage(message, error.message || "Unable to save your profile.");
    } finally {
      if (submitButton) submitButton.disabled = false;
    }
  });
});
