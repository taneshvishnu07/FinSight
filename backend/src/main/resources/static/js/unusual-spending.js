/* Loads unusual transactions and explains the model's comparison and detection reason. */
document.addEventListener("DOMContentLoaded",async()=>{try{
  if(isAnalysisRunning()){showAnalysisPendingPage();return;}
  const id=await resolveLatestUploadId();if(!id)throw new Error("Upload a transaction file first.");
  const d=await API.detectAnomalies(id);const list=d.anomalies||[];const average=Number(d.averageExpense||0);
  qs("#unusualList").innerHTML=list.map(x=>{
    const amount=Number(x.amount||0);const ratio=average>0?amount/average:null;
    return `<article class="alert-item unusual-detail-card"><div class="left"><div class="item-icon"><i class="bi bi-exclamation-triangle"></i></div><div><h3>${esc(x.merchant||"Unusual transaction")}</h3><p>${esc(formatDate(x.transactionDate))} · ${esc(x.category||"Uncategorised")}</p></div></div>
      <div class="item-value"><strong>${money(amount)}</strong><small class="severity ${String(x.severity||"MEDIUM").toLowerCase()}">${esc(x.severity||"MEDIUM")}</small></div>
      <div class="unusual-evidence"><div><span>Average transaction spending</span><strong>${money(average)}</strong></div><div><span>Comparison</span><strong>${ratio===null?"Not available":`${ratio.toFixed(1)}× average`}</strong></div><div class="unusual-reason"><span>Why it was detected</span><p>${esc(x.reason||"The detection model identified this transaction as different from the usual spending pattern.")}</p></div></div>
    </article>`;
  }).join("")||"<p class='muted'>No unusual spending detected.</p>";
  if(list.length) qs("#unusualList").insertAdjacentHTML("beforeend",'<p class="unusual-disclaimer">An unusual transaction is not automatically unnecessary. It is highlighted because its amount or pattern differs from the spending behaviour identified by the detection model.</p>');
}catch(error){console.error(error);qs("#unusualList").innerHTML=`<p class='muted'>${esc(error.message)}</p>`;}});
