/* FinSight File Notes: Loads and renders the main dashboard metrics, charts, comparisons, and financial summaries. */
document.addEventListener("DOMContentLoaded", async () => {
  const user = currentUser();
  if (isAnalysisRunning()) {
    showAnalysisPendingPage();
    return;
  }

  const firstName = user.fullName ? user.fullName.split(" ")[0] : user.name ? user.name.split(" ")[0] : "there";
  if (qs("#userName")) qs("#userName").textContent = esc(firstName);

  try {
    const [analysis, transactions] = await Promise.all([API.getLatestAnalysis(), API.getTransactions()]);
    const a = analysis || {};
    const tx = Array.isArray(transactions) ? transactions : [];

    qs("#incomeValue").textContent = money(a.totalIncome);
    qs("#expenseValue").textContent = money(a.totalExpense);
    qs("#savingsValue").textContent = money(a.totalSavings);
    qs("#largestExpense").textContent = money(a.largestExpense);
    qs("#largestExpenseLabel").textContent = a.largestExpenseDescription || "—";

    // Spending by Category: amounts stay in the dedicated legend; the doughnut
    // itself remains clean and its tooltip shows the category/value once.
    const cats = Object.entries(a.spendingByCategory || {}).filter(([, v]) => Number(v) > 0);
    const chartColors = ["#3B82F6", "#06B6D4", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#14B8A6", "#F97316", "#6366F1", "#84CC16", "#EAB308"];
    const categoryCanvas = qs("#categoryChart");
    if (categoryCanvas) {
      new Chart(categoryCanvas, {
        type: "doughnut",
        data: {
          labels: cats.map(([k]) => k),
          datasets: [{
            data: cats.map(([, v]) => Number(v)),
            backgroundColor: cats.map((_, i) => chartColors[i % chartColors.length]),
            borderColor: "#0B2442",
            borderWidth: 3
          }]
        },
        options: {
          maintainAspectRatio: false,
          layout: { padding: 6 },
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                title: () => "",
                label: context => `${context.label}: ${money(context.raw)}`
              }
            }
          }
        }
      });
    }
    const categoryLegend = qs("#categoryChartLegend");
    if (categoryLegend) {
      categoryLegend.innerHTML = cats.map(([k, v], i) => `
        <div class="category-legend-item">
          <span class="category-legend-dot" style="background:${chartColors[i % chartColors.length]}" aria-hidden="true"></span>
          <span class="category-legend-name">${esc(k)}</span>
          <strong>${money(v)}</strong>
        </div>`).join("") || '<span class="muted">No spending categories available.</span>';
    }

    const byMonth = {};
    tx.forEach(t => {
      const m = String(t.transactionDate || "").slice(0, 7);
      if (!m) return;
      byMonth[m] ||= { income: 0, expense: 0, savings: 0, categories: {} };
      const amount = Number(t.amount || 0);
      if (String(t.type) === "INCOME") {
        byMonth[m].income += amount;
      } else {
        byMonth[m].expense += amount;
        const c = t.category || "OTHER";
        byMonth[m].categories[c] = (byMonth[m].categories[c] || 0) + amount;
      }
      byMonth[m].savings = byMonth[m].income - byMonth[m].expense;
    });

    const ordered = Object.keys(byMonth).sort();
    const recentMonths = ordered.slice(-6);
    const monthLabel = key => {
      if (!key) return "—";
      const [year, month] = key.split("-");
      return new Date(Number(year), Number(month) - 1, 1).toLocaleDateString(undefined, { month: "long", year: "numeric" });
    };

    const trendCanvas = qs("#trendChart");
    if (trendCanvas) {
      new Chart(trendCanvas, {
        type: "line",
        data: {
          labels: recentMonths.map(monthLabel),
          datasets: [{
            label: "Expenses",
            data: recentMonths.map(m => byMonth[m].expense),
            tension: .35,
            fill: false,
            borderColor: "#38BDF8",
            backgroundColor: "#38BDF8",
            pointBackgroundColor: "#F59E0B",
            pointBorderColor: "#F8FAFC",
            pointRadius: 4,
            borderWidth: 3
          }]
        },
        options: {
          plugins: { legend: { display: false }, tooltip: { callbacks: { label: context => `Expenses: ${money(context.raw)}` } } },
          scales: {
            x: { ticks: { color: "#AFC8DF" }, grid: { color: "rgba(148,163,184,.12)" } },
            y: { beginAtZero: true, ticks: { color: "#AFC8DF", callback: value => money(value) }, grid: { color: "rgba(148,163,184,.12)" } }
          },
          maintainAspectRatio: false
        }
      });
    }

    qs("#recentTransactions").innerHTML = tx.slice(0, 8).map(t => `<tr><td>${esc(formatDate(t.transactionDate))}</td><td>${esc(t.description)}</td><td><span class="category-badge">${esc(t.category || "OTHER")}</span></td><td>${money(t.amount)}</td><td>${esc(t.type || "EXPENSE")}</td></tr>`).join("") || `<tr><td colspan="5">No transactions available.</td></tr>`;

    const comparisonBox = qs("#monthlyComparison");
    const savingsRateCanvas = qs("#monthlySavingsRateChart");
    if (ordered.length >= 2) {
      const currentKey = ordered.at(-1);
      const previousKey = ordered.at(-2);
      const cur = byMonth[currentKey];
      const prev = byMonth[previousKey];
      const currentLabel = monthLabel(currentKey);
      const previousLabel = monthLabel(previousKey);
      const savingsChange = cur.savings - prev.savings;
      const savingsPct = prev.savings > 0 ? (savingsChange / prev.savings) * 100 : null;
      const expenseChangePct = prev.expense > 0 ? ((cur.expense - prev.expense) / prev.expense) * 100 : null;

      const allCategories = [...new Set([...Object.keys(prev.categories), ...Object.keys(cur.categories)])].sort((a, b) => a.localeCompare(b));
      const rows = allCategories.map(category => {
        const previous = Number(prev.categories[category] || 0);
        const current = Number(cur.categories[category] || 0);
        const delta = previous - current;
        let changeText = "No change";
        let className = "comparison-neutral";
        if (delta > 0.009) {
          const pct = previous > 0 ? (delta / previous) * 100 : 0;
          changeText = `${money(delta)} lower${pct ? ` (${pct.toFixed(1)}% lower)` : ""}`;
          className = "comparison-lower";
        } else if (delta < -0.009) {
          const increase = Math.abs(delta);
          const pct = previous > 0 ? (increase / previous) * 100 : 0;
          changeText = `${money(increase)} higher${pct ? ` (${pct.toFixed(1)}% higher)` : ""}`;
          className = "comparison-higher";
        } else if (current > 0 && previous === 0) {
          changeText = `New spending (${money(current)})`;
          className = "comparison-higher";
        }
        return `<tr><td>${esc(category)}</td><td>${money(previous)}</td><td>${money(current)}</td><td class="${className}"><strong>${esc(changeText)}</strong></td></tr>`;
      }).join("");

      const lowerCategories = allCategories.map(category => {
        const previous = Number(prev.categories[category] || 0);
        const current = Number(cur.categories[category] || 0);
        return { category, saved: previous - current };
      }).filter(x => x.saved > 0.009).sort((a, b) => b.saved - a.saved);
      const higherCategories = allCategories.map(category => {
        const previous = Number(prev.categories[category] || 0);
        const current = Number(cur.categories[category] || 0);
        return { category, increase: current - previous };
      }).filter(x => x.increase > 0.009).sort((a, b) => b.increase - a.increase);

      const lowerSummary = lowerCategories.length
        ? `You spent less in ${lowerCategories.length} categor${lowerCategories.length === 1 ? "y" : "ies"}: ${lowerCategories.slice(0, 3).map(x => `${esc(x.category)} (${money(x.saved)} lower)`).join(", ")}${lowerCategories.length > 3 ? ", and more." : "."}`
        : `You did not reduce spending in any category in ${currentLabel}.`;
      const higherSummary = higherCategories.length
        ? `Spending increased in ${higherCategories.length} categor${higherCategories.length === 1 ? "y" : "ies"}: ${higherCategories.slice(0, 3).map(x => `${esc(x.category)} (${money(x.increase)} higher)`).join(", ")}${higherCategories.length > 3 ? ", and more." : "."}`
        : `No category had higher spending in ${currentLabel}.`;

      const savingsSentence = prev.savings > 0 && savingsPct !== null
        ? `You saved ${money(cur.savings)} in ${currentLabel}, ${savingsPct >= 0 ? "an increase" : "a decrease"} of ${Math.abs(savingsPct).toFixed(1)}% compared with ${previousLabel}.`
        : `You saved ${money(cur.savings)} in ${currentLabel}. Your savings changed by ${savingsChange >= 0 ? "increasing" : "decreasing"} ${money(Math.abs(savingsChange))} compared with ${previousLabel}.`;
      const expenseSentence = expenseChangePct === null
        ? `There was not enough previous-month expense data to calculate an expense percentage.`
        : `Your total expenses ${expenseChangePct <= 0 ? "fell" : "increased"} by ${money(Math.abs(cur.expense - prev.expense))} (${Math.abs(expenseChangePct).toFixed(1)}%) from ${previousLabel} to ${currentLabel}.`;

      comparisonBox.innerHTML = `
        <div class="comparison-heading"><strong>${esc(currentLabel)} compared with ${esc(previousLabel)}</strong><span>Every category is included below.</span></div>
        <div class="comparison-metrics">
          <div class="comparison-stat"><span>How much you saved</span><strong>${money(cur.savings)}</strong><small>${esc(currentLabel)}</small></div>
          <div class="comparison-stat"><span>Change in savings</span><strong>${savingsChange >= 0 ? "+" : "-"}${money(Math.abs(savingsChange))}</strong><small>${savingsPct === null ? "Percentage not available" : `${Math.abs(savingsPct).toFixed(1)}% vs ${esc(previousLabel)}`}</small></div>
          <div class="comparison-stat"><span>Total expenses</span><strong>${money(cur.expense)}</strong><small>${esc(currentLabel)}</small></div>
          <div class="comparison-stat"><span>Expense change</span><strong>${expenseChangePct === null ? "N/A" : `${expenseChangePct >= 0 ? "+" : ""}${expenseChangePct.toFixed(1)}%`}</strong><small>vs ${esc(previousLabel)}</small></div>
        </div>
        <div class="comparison-summary"><p>${savingsSentence}</p><p>${expenseSentence}</p><p>${lowerSummary}</p><p>${higherSummary}</p></div>
        <div class="comparison-table-wrap">
          <table class="comparison-table">
            <thead><tr><th>Category</th><th>${esc(previousLabel)}</th><th>${esc(currentLabel)}</th><th>Change</th></tr></thead>
            <tbody>${rows || `<tr><td colspan="4">No category spending data available.</td></tr>`}</tbody>
          </table>
        </div>`;

      if (savingsRateCanvas) {
        new Chart(savingsRateCanvas, {
          type: "line",
          data: {
            labels: recentMonths.map(monthLabel),
            datasets: [{
              label: "Savings rate",
              data: recentMonths.map(m => {
                const income = byMonth[m].income;
                return income > 0 ? (byMonth[m].savings / income) * 100 : 0;
              }),
              tension: .3,
              borderColor: "#10B981",
              backgroundColor: "rgba(16,185,129,.14)",
              pointBackgroundColor: "#F59E0B",
              pointBorderColor: "#F8FAFC",
              pointRadius: 4,
              borderWidth: 3,
              fill: true
            }]
          },
          options: {
            maintainAspectRatio: false,
            plugins: { legend: { display: false }, tooltip: { callbacks: { label: context => `Savings rate: ${Number(context.raw).toFixed(1)}%` } } },
            scales: {
              x: { ticks: { color: "#AFC8DF" }, grid: { color: "rgba(148,163,184,.12)" } },
              y: { beginAtZero: true, ticks: { color: "#AFC8DF", callback: value => `${value}%` }, grid: { color: "rgba(148,163,184,.12)" } }
            }
          }
        });
      }
    } else {
      comparisonBox.innerHTML = '<p class="muted">At least two months of transactions are needed for comparison analysis.</p>';
      if (savingsRateCanvas) savingsRateCanvas.parentElement.innerHTML = '<p class="muted">Add at least two months of transactions to view the monthly savings rate.</p>';
    }
  } catch (error) {
    console.error(error);
    const comparisonBox = qs("#monthlyComparison");
    if (comparisonBox) comparisonBox.innerHTML = `<p class="muted">${esc(error.message || "Unable to load dashboard comparison analysis.")}</p>`;
  }

  window.FinSightAlerts?.showOnDashboard();
});

// First-dashboard-login announcement for the built-in user guide.
document.addEventListener("DOMContentLoaded", () => {
  const guideUser = currentUser();
  const guideKey = `finsight_user_guide_announcement_seen_${guideUser.email || guideUser.id || 'current-user'}`;
  const firstSetupPending = localStorage.getItem('finsight_show_user_guide_announcement') === 'true';
  if (!firstSetupPending && localStorage.getItem(guideKey) === 'true') return;

  const mount = document.createElement('div');
  mount.id = 'guideAnnouncement';
  mount.innerHTML = `
    <div class="guide-announcement-overlay">
      <section class="guide-announcement" role="dialog" aria-modal="true" aria-labelledby="guideAnnouncementTitle">
        <button type="button" id="closeGuideAnnouncement" class="guide-announcement-close" aria-label="Close announcement">×</button>
        <span class="eyebrow">WELCOME TO FINSIGHT</span>
        <h2 id="guideAnnouncementTitle">A user guide is available</h2>
        <p>The FinSight User Guide explains the main steps for using your dashboard, uploading transactions and viewing your financial analysis.</p>
        <div class="guide-announcement-actions">
          <button type="button" id="viewGuideAnnouncement" class="btn btn-dark">View User Guide</button>
          <button type="button" id="laterGuideAnnouncement" class="btn btn-light">Later</button>
        </div>
      </section>
    </div>`;
  document.body.appendChild(mount);

  const close = () => {
    localStorage.setItem(guideKey, 'true');
    localStorage.removeItem('finsight_show_user_guide_announcement');
    mount.remove();
  };
  qs('#viewGuideAnnouncement')?.addEventListener('click', () => {
    close();
    window.FinSightGuide?.open();
  });
  qs('#laterGuideAnnouncement')?.addEventListener('click', close);
  qs('#closeGuideAnnouncement')?.addEventListener('click', close);
});
