/* FinSight File Notes: Loads and displays personalised recommendations and their generation basis. */
function recommendationBasis(type) {
  const map = {
    BUDGET_ADJUSTMENT: "Budget",
    FINANCIAL_GOAL: "Financial goal",
    INCREASE_SAVINGS: "Savings",
    CASHFLOW_IMPROVEMENT: "Cash flow",
    REDUCE_SPENDING: "Spending behaviour",
    SUBSCRIPTION_OPTIMIZATION: "Subscriptions",
    OTHER: "Overall spending"
  };
  return map[String(type || "OTHER").toUpperCase()] || "Financial activity";
}

document.addEventListener("DOMContentLoaded", async () => {
  try {
    if (isAnalysisRunning()) { showAnalysisPendingPage(); return; }
    const id = await resolveLatestUploadId();
    if (!id) throw new Error("Upload transactions first to generate recommendations.");
    const d = await API.getRecommendations(id);
    const list = d.recommendations || [];
    qs("#recommendationGrid").innerHTML = list.map(x => `
      <article class="recommendation-card">
        <div class="top">
          <div class="recommendation-badges">
            <span class="severity ${String(x.priority||"LOW").toLowerCase()}">${esc(x.priority || "LOW")}</span>
            <span class="recommendation-basis"><i class="bi bi-tag"></i> ${esc(recommendationBasis(x.type))}</span>
          </div>
          <i class="bi bi-lightbulb"></i>
        </div>
        <h3>${esc(x.title || "Financial recommendation")}</h3>
        <p>${esc(x.message || "Review your recent financial activity.")}</p>
        <footer>Generated from ${esc(recommendationBasis(x.type).toLowerCase())} criteria</footer>
      </article>`).join("") || "<p class='muted'>No recommendations available yet.</p>";
  } catch (error) {
    console.error(error);
    qs("#recommendationGrid").innerHTML = `<p class='muted'>${esc(error.message)}</p>`;
  }
});
