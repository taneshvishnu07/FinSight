/* Shared financial-health and insight presentation for Dashboard and Financial Report. */
window.FinSightHealth = (() => {
  const statusMeta = status => {
    const key = String(status || '').toUpperCase();
    if (key === 'EXCELLENT' || key === 'GOOD') return {label:key === 'EXCELLENT' ? 'Excellent' : 'Good', tone:'good', icon:'bi-check-circle-fill'};
    if (key === 'FAIR') return {label:'Fair', tone:'fair', icon:'bi-exclamation-circle-fill'};
    return {label:'Needs Attention', tone:'low', icon:'bi-exclamation-triangle-fill'};
  };
  const moneyValue = value => typeof money === 'function' ? money(value) : `RM${Number(value || 0).toFixed(2)}`;
  const safe = value => typeof esc === 'function' ? esc(value) : String(value ?? '');

  function renderHealth(targetId, data, analysis) {
    const target = document.getElementById(targetId);
    if (!target) return;
    if (!data || !Number.isFinite(Number(data.financialHealthScore))) {
      target.innerHTML = '<p class="muted">Financial health information is not available yet. Upload transactions and run an analysis to view your score.</p>';
      return;
    }
    const score = Math.max(0, Math.min(100, Number(data.financialHealthScore)));
    const meta = statusMeta(data.financialHealthStatus);
    const savingsRate = Number(data.savingsRate || 0);
    const income = Number(analysis?.totalIncome || 0);
    const expense = Number(analysis?.totalExpense || 0);
    const expenseRate = income > 0 ? (expense / income) * 100 : 0;
    const savings = Number(analysis?.totalSavings ?? (income - expense));
    const color = meta.tone === 'good' ? '#278052' : meta.tone === 'fair' ? '#B78320' : '#B94A48';
    const reasons = [];
    if (income > 0 && savingsRate < 10) reasons.push(`Your savings rate is ${savingsRate.toFixed(2)}%, which means less than 10% of your income was retained as savings.`);
    if (income > 0 && expenseRate > 90) reasons.push(`${expenseRate.toFixed(2)}% of your income was used for expenses, leaving a limited amount available for savings.`);
    if (savings < 0) reasons.push(`Your expenses exceeded your income by ${moneyValue(Math.abs(savings))}, resulting in a monthly deficit over the analysed period.`);
    if (reasons.length === 0 && savingsRate >= 20 && expenseRate <= 80 && savings >= 0) reasons.push('Your savings rate and expense-to-income ratio indicate a relatively healthy balance between income and spending.');
    if (reasons.length === 0) reasons.push('Review your savings rate, expense-to-income ratio and monthly cash flow to understand what is influencing this score.');
    target.innerHTML = `
      <div class="health-score-layout">
        <div class="health-score-wrap"><div class="health-score-ring" style="--health-progress:${score * 3.6}deg;--health-color:${color}" role="img" aria-label="Financial health score ${score} out of 100"><div class="health-score-inner"><span class="health-score-number">${score}</span><span class="muted">out of 100</span></div></div></div>
        <div class="health-score-details">
          <span class="health-status ${meta.tone}"><i class="bi ${meta.icon}"></i>${meta.label}</span>
          <p class="health-summary">${safe(data.overallSummary || 'Your financial health summary is based on your latest transaction analysis.')}</p>
          <div class="health-stat-grid">
            <div class="health-stat"><span>Savings rate</span><strong>${savingsRate.toFixed(2)}%</strong></div>
            <div class="health-stat"><span>Income used</span><strong>${expenseRate.toFixed(2)}%</strong></div>
            <div class="health-stat"><span>Total savings</span><strong>${moneyValue(savings)}</strong></div>
          </div>
          <div class="health-reasons"><strong>What is affecting your score?</strong><ul>${reasons.map(reason => `<li>${safe(reason)}</li>`).join('')}</ul></div>
          <a class="health-recommendation-link" href="recommendations.html"><i class="bi bi-lightbulb"></i> View personalised recommendations <i class="bi bi-arrow-right"></i></a>
          <p class="health-score-note">These explanations use your analysed income, expenses and savings. The financial health score itself follows FinSight's existing assessment logic.</p>
        </div>
      </div>`;
  }

  function renderPriority(targetId, data, analysis) {
    const target = document.getElementById(targetId);
    if (!target) return;
    if (!data && !analysis) {
      target.innerHTML = '<p class="muted">Insights will appear after transactions have been analysed.</p>';
      return;
    }
    const all = Array.isArray(data?.insights) ? data.insights : [];
    const findInsight = kind => all.find(item => {
      const type = String(item.type || '').toUpperCase();
      const title = String(item.title || '').toUpperCase();
      return kind === 'unusual'
        ? (type.includes('UNUSUAL') || type.includes('ANOMAL') || title.includes('UNUSUAL'))
        : (type.includes('SUBSCRIPTION') || type.includes('RECURR') || title.includes('RECURR') || title.includes('SUBSCRIPTION'));
    });
    const unusualItem = findInsight('unusual');
    const recurringItem = findInsight('recurring');
    const unusualCount = Number(analysis?.unusualTransactions || 0);
    const recurringCount = Number(analysis?.recurringSubscriptions || 0);
    const cards = [];

    if (unusualItem || unusualCount > 0) {
      cards.push({
        category:'Unusual spending', icon:'bi-exclamation-triangle', priority:String(unusualItem?.priority || 'HIGH').toUpperCase(),
        title:unusualItem?.title || 'Unusual transactions detected',
        message:unusualItem?.message || `${unusualCount} transaction${unusualCount === 1 ? '' : 's'} stood out from your usual spending pattern. Review the transaction details before deciding whether any action is needed. Unusual spending does not automatically mean unnecessary spending.`,
        amount:unusualItem?.relatedAmount != null ? moneyValue(unusualItem.relatedAmount) : '',
        context:[unusualItem?.relatedCategory, unusualItem?.relatedMerchant].filter(Boolean).join(' · '),
        href:'unusual-spending.html', link:'Review unusual spending'
      });
    }
    if (recurringItem || recurringCount > 0) {
      cards.push({
        category:'Recurring cost', icon:'bi-repeat', priority:String(recurringItem?.priority || 'MEDIUM').toUpperCase(),
        title:recurringItem?.title || 'Recurring payments identified',
        message:recurringItem?.message || `FinSight identified ${recurringCount} recurring payment${recurringCount === 1 ? '' : 's'}. Review whether each service is still useful and consider its monthly and annual cost.`,
        amount:recurringItem?.relatedAmount != null ? moneyValue(recurringItem.relatedAmount) : '',
        context:[recurringItem?.relatedCategory, recurringItem?.relatedMerchant].filter(Boolean).join(' · '),
        href:'subscriptions.html', link:'Review recurring payments'
      });
    }
    if (!cards.length) {
      target.innerHTML = '<div class="insight-empty"><strong>No unusual spending or recurring costs identified.</strong><p>These priority insights will appear when your transaction analysis detects unusual activity or recurring payments.</p></div>';
      return;
    }
    target.innerHTML = cards.map((item,index) => `
      <article class="priority-insight-card priority-${safe(item.priority.toLowerCase())}">
        <div class="priority-insight-top"><span class="priority-insight-category"><i class="bi ${safe(item.icon)}"></i>${safe(item.category)}</span><span class="priority-insight-number">${String(index+1).padStart(2,'0')}</span></div>
        <h3>${safe(item.title)}</h3>
        <p>${safe(item.message)}</p>
        ${item.amount || item.context ? `<div class="priority-insight-context">${item.amount ? `<strong>${safe(item.amount)}</strong>` : ''}${item.context ? `<span>${safe(item.context)}</span>` : ''}</div>` : ''}
        <a class="priority-action-link" href="${safe(item.href)}">${safe(item.link)} <i class="bi bi-arrow-right"></i></a>
      </article>`).join('');
  }

  return {renderHealth, renderPriority};
})();
