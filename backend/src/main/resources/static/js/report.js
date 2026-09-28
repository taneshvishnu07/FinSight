/* FinSight File Notes: Loads and renders the detailed financial report, charts, forecasts, insights, recommendations, and alerts. */
async function loadReport(){
  try{
    if (isAnalysisRunning()) { showAnalysisPendingPage(); return; }
    const uploadId=Number(new URLSearchParams(location.search).get("uploadHistoryId")||await resolveLatestUploadId());
    const [a,txs,insights]=await Promise.all([API.getLatestAnalysis(),API.getTransactions(),API.getInsights().catch(()=>null)]);
    if(uploadId)localStorage.setItem("finsight_last_upload_id",String(uploadId));
    qs("#incomeValue").textContent=money(a.totalIncome);qs("#expenseValue").textContent=money(a.totalExpense);qs("#savingsValue").textContent=money(a.totalSavings);qs("#averageValue").textContent=money(a.averageExpense);
    const tx=Array.isArray(txs)?txs:[];const monthly={};tx.filter(t=>String(t.type)==="EXPENSE").forEach(t=>{const k=String(t.transactionDate||"").slice(0,7);if(k)monthly[k]=(monthly[k]||0)+Number(t.amount||0);});const months=Object.entries(monthly).sort(([x],[y])=>x.localeCompare(y));
    new Chart(qs("#reportTrend"),{type:"line",data:{labels:months.map(([m])=>{const [y,mo]=m.split("-");return new Date(Number(y),Number(mo)-1,1).toLocaleDateString(undefined,{month:"short",year:"numeric"});}),datasets:[{label:"Expenses",data:months.map(([,v])=>v),tension:.35,borderColor:"#38BDF8",backgroundColor:"rgba(56,189,248,.18)",pointBackgroundColor:"#F59E0B",pointBorderColor:"#F8FAFC",pointRadius:4,borderWidth:3,fill:true}]},options:{maintainAspectRatio:false,plugins:{legend:{display:false},tooltip:{callbacks:{label:context=>`Expenses: ${money(context.raw)}`}}},scales:{x:{ticks:{color:"#AFC8DF"},grid:{color:"rgba(148,163,184,.12)"}},y:{beginAtZero:true,ticks:{color:"#AFC8DF",callback:value=>money(value)},grid:{color:"rgba(148,163,184,.12)"}}}}});
    const cats=Object.entries(a.spendingByCategory||{}).filter(([,v])=>Number(v)>0);
    const chartColors=["#3B82F6","#06B6D4","#10B981","#F59E0B","#EF4444","#8B5CF6","#EC4899","#14B8A6","#F97316","#6366F1","#84CC16","#EAB308"];
    const reportCanvas=qs("#reportCategory");
    if(reportCanvas){new Chart(reportCanvas,{type:"doughnut",data:{labels:cats.map(([k])=>k),datasets:[{data:cats.map(([,v])=>Number(v)),backgroundColor:cats.map((_,i)=>chartColors[i%chartColors.length]),borderColor:"#0B2442",borderWidth:3}]},options:{maintainAspectRatio:false,layout:{padding:8},plugins:{legend:{display:false},tooltip:{callbacks:{title:()=>[],label:context=>context.label+": "+money(context.raw)}}}}});}
    const reportLegend=qs("#reportCategoryLegend");
    if(reportLegend){reportLegend.innerHTML=cats.map(([k,v],i)=>`<div class="category-legend-item"><span class="category-legend-dot" style="background:${chartColors[i%chartColors.length]}"></span><span class="category-legend-name">${esc(k)}</span><strong>${money(v)}</strong></div>`).join("")||'<span class="muted">No spending categories available.</span>';}
    const list=[];if(a.largestExpenseDescription)list.push(["Largest expense",`${a.largestExpenseDescription} · ${money(a.largestExpense)}`]);list.push(["Subscriptions",`${a.recurringSubscriptions||0} recurring subscription${Number(a.recurringSubscriptions)===1?"":"s"}.`]);list.push(["Unusual spending",`${a.unusualTransactions||0} transaction${Number(a.unusualTransactions)===1?"":"s"} need review.`]);
    if(insights?.overallSummary)list.push(["Financial health",insights.overallSummary]);
    qs("#keyInsights").innerHTML=list.map(x=>`<div class="insight-item"><strong>${esc(x[0])}</strong><p>${esc(x[1])}</p></div>`).join("");
    if(uploadId){
      try{const f=await API.forecast(uploadId,3);const rows=f.forecastMonths||[];qs("#forecastTable").innerHTML=rows.map(x=>`<tr><td>${esc(x.month)}</td><td><strong>${money(x.predictedExpense)}</strong></td><td>${money(x.lowerBound)} – ${money(x.upperBound)}</td></tr>`).join("")||`<tr><td colspan="3">No forecast available.</td></tr>`;}catch(error){qs("#forecastTable").innerHTML=`<tr><td colspan="3">${esc(error.message)}</td></tr>`;}
      try{const r=await API.getRecommendations(uploadId);const recs=r.recommendations||[];qs("#recommendationPreview").innerHTML=recs.slice(0,3).map(x=>`<div class="mini-item"><i class="bi bi-lightbulb"></i><div><strong>${esc(x.title)}</strong><p>${esc(x.message)}</p></div></div>`).join("")||"<p class='muted'>No recommendations yet.</p>";}catch{}
      try{const r=await API.getAlerts(uploadId);const alerts=r.alerts||[];qs("#alertPreview").innerHTML=alerts.slice(0,3).map(x=>`<div class="mini-item"><i class="bi bi-bell"></i><div><strong>${esc(x.title)}</strong><p>${esc(x.message)}</p></div></div>`).join("")||"<p class='muted'>No alerts.</p>";}catch{}
    }
  }catch(error){console.error(error);}
  window.FinSightAlerts?.showOnDashboard();
}
document.addEventListener("DOMContentLoaded",()=>{loadReport();qs("#refreshReport")?.addEventListener("click",loadReport);});
