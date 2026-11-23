/* FinSight File Notes: Handles registration, login, password visibility, and first-time login navigation. */
document.addEventListener("DOMContentLoaded", () => {
  const login = qs("#loginForm");
  const register = qs("#registerForm");

  // A newly registered user sees a first-login experience focused on
  // completing the financial profile before entering the dashboard.
  const firstLogin = localStorage.getItem("finsight_first_login_pending") === "true";
  const heading = document.querySelector(".auth-card h1");
  const loginButton = login?.querySelector('button[type="submit"]');
  if (firstLogin) {
    if (heading) heading.textContent = "Welcome";
    if (loginButton) loginButton.textContent = "Login to Financial Profile Setting";
  }

  function validateFullName(value) {
    return /^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ '\\-][A-Za-zÀ-ÖØ-öø-ÿ]+)+$/.test(value.trim());
  }

  function passwordChecks(password) {
    return {
      length: password.length >= 8,
      lowercase: /[a-z]/.test(password),
      uppercase: /[A-Z]/.test(password),
      number: /\d/.test(password),
      special: /[^A-Za-z0-9]/.test(password)
    };
  }

  function passwordStrength(password) {
    const checks = passwordChecks(password);
    const score = Object.values(checks).filter(Boolean).length;
    if (!password) return { label: "Enter a password", score: 0 };
    if (score <= 2) return { label: "Weak", score };
    if (score === 3 || score === 4) return { label: "Medium", score };
    return { label: "Strong", score };
  }

  const password = qs("#password");
  const strengthLabel = qs("#passwordStrengthLabel");
  const strengthBar = qs("#passwordStrengthBar");
  const passwordRequirements = qs("#passwordRequirements");

  function updatePasswordStrength() {
    if (!password || !strengthLabel || !strengthBar) return;
    const value = password.value;
    const strength = passwordStrength(value);
    strengthLabel.textContent = strength.label;
    strengthBar.style.width = `${Math.min(100, strength.score * 20)}%`;

    if (passwordRequirements) {
      const checks = passwordChecks(value);
      passwordRequirements.querySelectorAll("[data-check]").forEach(item => {
        const valid = checks[item.dataset.check];
        item.classList.toggle("valid", valid);
        const icon = item.querySelector("i");
        if (icon) icon.className = `bi ${valid ? "bi-check-circle-fill" : "bi-circle"}`;
      });
    }
  }

  password?.addEventListener("input", updatePasswordStrength);
  updatePasswordStrength();

  login?.addEventListener("submit", async event => {
    event.preventDefault();
    const message = qs("#formMessage");
    const email = qs("#email").value.trim().toLowerCase();
    const passwordValue = qs("#password").value;

    if (!email || !/^\S+@\S+\.\S+$/.test(email)) {
      setMessage(message, "Please enter a valid email address.");
      return;
    }
    if (!passwordValue) {
      setMessage(message, "Please enter your password.");
      return;
    }

    try {
      setMessage(message, "Signing in...");
      const data = await API.login({ email, password: passwordValue });
      if (!data.token) throw new Error("Login succeeded but no access token was returned.");

      setToken(data.token);
      saveUser({ userId: data.userId, fullName: data.fullName, email: data.email });
      localStorage.setItem("finsight_profile_completed", String(!!data.profileCompleted));
      localStorage.setItem("finsight_profile_verified_at", String(Date.now()));
      localStorage.removeItem("finsight_first_login_pending");

      location.href = data.profileCompleted ? "dashboard.html" : "profile.html";
    } catch (err) {
      setMessage(message, err.message);
    }
  });

  register?.addEventListener("submit", async event => {
    event.preventDefault();
    const message = qs("#formMessage");
    const fullName = qs("#name").value.trim();
    const email = qs("#email").value.trim().toLowerCase();
    const passwordValue = qs("#password").value;
    const confirmPassword = qs("#confirmPassword").value;

    if (!validateFullName(fullName)) {
      setMessage(message, "Please enter your full name, such as John Smith. Use letters, spaces, hyphens or apostrophes only.");
      return;
    }
    if (!/^\S+@\S+\.\S+$/.test(email)) {
      setMessage(message, "Please enter a valid email address.");
      return;
    }

    const checks = passwordChecks(passwordValue);
    if (!Object.values(checks).every(Boolean)) {
      setMessage(message, "Please create a strong password that meets all the requirements shown below.");
      return;
    }
    if (passwordValue !== confirmPassword) {
      setMessage(message, "Passwords do not match.");
      return;
    }

    try {
      setMessage(message, "Creating your account...");
      await API.register({ fullName, email, password: passwordValue });
      localStorage.setItem("finsight_first_login_pending", "true");
      setMessage(message, "Account created successfully. Redirecting to login...", true);
      setTimeout(() => location.href = "login.html", 800);
    } catch (err) {
      setMessage(message, err.message);
    }
  });
});
