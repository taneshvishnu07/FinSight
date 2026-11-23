/* Displays recurring costs and calculates potential savings from services users no longer use. */
document.addEventListener("DOMContentLoaded", async () => {
  try {
    if (isAnalysisRunning()) { showAnalysisPendingPage(); return; }
    const d = await API.detectSubscriptions();
    const list = d.subscriptions || [];
    qs("#subscriptionCount").textContent = d.subscriptionCount ?? list.length;
    qs("#monthlyCost").textContent = money(d.estimatedMonthlySubscriptionCost);
    qs("#yearlyCost").textContent = money(d.estimatedYearlySubscriptionCost);
    const storageKey = "finsight_subscription_review_v1";
    let review = {};
    try { review = JSON.parse(localStorage.getItem(storageKey) || "{}"); } catch (_) { review = {}; }
    const escapeKey = value => String(value || "").replace(/[^a-zA-Z0-9_-]/g, "_");
    const monthlyOf = x => Number(x.estimatedMonthlyCost || 0);
    const renderSavings = () => {
      const candidates = list.filter(x => review[escapeKey(x.merchant)] === "unused");
      const monthly = candidates.reduce((sum, x) => sum + monthlyOf(x), 0);
      const box = document.getElementById("potentialSubscriptionSavings");
      if (box) box.innerHTML = `<strong>Potential savings from services you no longer use</strong><p>${money(monthly)} per month · ${money(monthly * 12)} per year</p><small>Estimate based on the services you marked as no longer used. Savings are not guaranteed until the subscriptions are cancelled.</small>`;
    };
    qs("#subscriptionList").innerHTML = list.map((x, index) => {
      const monthly = monthlyOf(x);
      const annual = Number(x.estimatedYearlyCost || monthly * 12);
      const key = escapeKey(x.merchant);
      const selected = review[key] === "unused";
      return `<article class="subscription-item">
        <div class="left"><div class="item-icon"><i class="bi bi-repeat"></i></div><div><h3>${esc(x.merchant)}</h3><p>${esc(x.frequency)} · ${x.occurrences} detected occurrence${Number(x.occurrences) === 1 ? "" : "s"}</p></div></div>
        <div class="item-value"><strong>${money(monthly)}</strong><small>per month</small><strong>${money(annual)}</strong><small>per year</small></div>
        <div class="subscription-review"><strong>Are you still using this service?</strong><p>If you no longer need it, cancelling could save ${money(monthly)} per month (${money(annual)} per year).</p>
          <label class="subscription-choice"><input type="checkbox" data-subscription-index="${index}" ${selected ? "checked" : ""}> I no longer use this service</label>
        </div>
      </article>`;
    }).join("") || "<p class='muted'>No recurring payments were detected in your transaction history.</p>";
    qs("#subscriptionList").querySelectorAll("[data-subscription-index]").forEach(input => input.addEventListener("change", event => {
      const item = list[Number(event.target.dataset.subscriptionIndex)];
      const key = escapeKey(item.merchant);
      if (event.target.checked) review[key] = "unused"; else delete review[key];
      localStorage.setItem(storageKey, JSON.stringify(review));
      renderSavings();
    }));
    renderSavings();
  } catch (error) { console.error(error); qs("#subscriptionList").innerHTML = `<p class="muted">Unable to load recurring payments: ${esc(error.message || "Please try again later.")}</p>`; }
});
