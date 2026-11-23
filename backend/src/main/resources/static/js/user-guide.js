/* FinSight File Notes: Provides the first-time user guide announcement and the in-app guide content. */
(function(){
  const steps = [
    {
      title: "1. Complete your Financial Profile",
      text: "Enter your monthly budget, financial goal period and occupation details. Save the profile to unlock the FinSight dashboard."
    },
    {
      title: "2. Upload your transaction file",
      text: "Go to Transactions and upload one CSV file containing at least 3 months of transaction history. FinSight checks the file format, required columns and transaction period before upload."
    },
    {
      title: "3. Start the Financial Analysis",
      text: "After the upload is accepted, FinSight runs its specialised analysis stages for transaction classification, spending behaviour, subscriptions, forecasting, unusual spending, recommendations and alerts. Wait until the analysis is completed."
    },
    {
      title: "4. View your analysis results",
      text: "After analysis finishes, click View Analysis. Review your financial summary, spending trends, recommendations, subscriptions, unusual spending and alerts. FinSight will show an alert popup when there are alerts from the completed analysis."
    }
  ];

  function render(stepIndex){
    const mount = qs("#userGuideMount");
    if(!mount) return;
    const i = Math.max(0, Math.min(stepIndex, steps.length - 1));
    const step = steps[i];
    mount.innerHTML = `
      <div class="guide-overlay" role="dialog" aria-modal="true" aria-labelledby="guideTitle">
        <section class="guide-modal">
          <div class="guide-header">
            <div>
              <span class="eyebrow">FINSIGHT USER GUIDE</span>
              <h2 id="guideTitle">${esc(step.title)}</h2>
            </div>
            <button type="button" class="guide-close" id="guideClose" aria-label="Close user guide">×</button>
          </div>
          <div class="guide-progress" aria-label="Guide progress">
            ${steps.map((_,n)=>`<span class="${n===i?'active':''}"></span>`).join("")}
          </div>
          <p class="guide-description">${esc(step.text)}</p>
          <div class="guide-tip"><i class="bi bi-info-circle"></i><span>Use the sidebar to return to any section after completing the main workflow.</span></div>
          <div class="guide-actions">
            <button type="button" id="guideBack" ${i===0?'disabled':''}>Back</button>
            <div>
              <button type="button" id="guideCloseBottom" class="secondary">Close</button>
              ${i < steps.length-1 ? '<button type="button" id="guideNext" class="primary">Next</button>' : '<button type="button" id="guideDone" class="primary">Done</button>'}
            </div>
          </div>
        </section>
      </div>`;

    qs("#guideClose")?.addEventListener("click", close);
    qs("#guideCloseBottom")?.addEventListener("click", close);
    qs("#guideBack")?.addEventListener("click", ()=>render(i-1));
    qs("#guideNext")?.addEventListener("click", ()=>render(i+1));
    qs("#guideDone")?.addEventListener("click", close);
  }

  function close(){
    qs("#userGuideMount")?.replaceChildren();
  }

  window.FinSightGuide = {
    open(){ render(0); }
  };

  document.addEventListener("DOMContentLoaded", ()=>{
    // The user guide is opened only when the user selects User Guide.
    // No automatic product tour is shown.
  });
})();
