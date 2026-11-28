/* FinSight File Notes: Controls the analysis page, starts an analysis job, monitors its status, and opens completed results. */
document.addEventListener("DOMContentLoaded", async () => {
  // Analysis page: reads ?uploadHistoryId= (falls back to the last upload) and runs
  // the whole backend pipeline with ONE call, API.runAnalysis(). The agent cards and
  // progress bar are a visual animation only - they are not driven by real per-step
  // progress from the server.
  const params=new URLSearchParams(location.search);
  const uploadId=Number(params.get("uploadHistoryId")||latestUploadIdFromStorage());
  const steps=qsa(".agent-card"); const bar=qs("#analysisBar"); const percent=qs("#analysisPercent"); const status=qs("#analysisStatus"); const view=qs("#viewReport");
  if(!uploadId){status.textContent="No upload selected";return;}
  localStorage.setItem("finsight_last_upload_id",String(uploadId));
  // Mark the analysis as running so other pages show a waiting notice instead of half-
  // finished data; cleared on success or failure below.
  setAnalysisRunning(true, uploadId);
  let completed=false;
  function paint(n){const pct=Math.min(100,Math.round(n/steps.length*100));bar.style.width=pct+"%";percent.textContent=pct+"%";status.textContent=pct>=100?"Analysis completed":"Processing agents...";steps.forEach((card,i)=>{const pill=card.querySelector(".status-pill");pill.className="status-pill "+(i<n?"completed":i===n?"processing":"processing");pill.textContent=i<n?"COMPLETED":i===n?"PROCESSING":"QUEUED";});}
  paint(0);
  const animation=setInterval(()=>{if(!completed){const done=steps.filter(c=>c.querySelector(".status-pill")?.textContent==="COMPLETED").length;paint(Math.min(done+1,steps.length-1));}},650);
  try{
    status.textContent="Running all FinSight agents...";
    const result=await API.runAnalysis(uploadId);
    // Cache the result (and its alerts) so other pages can use it, and flag that the
    // alert popup should appear on the report page.
    localStorage.setItem("finsight_last_analysis",JSON.stringify(result));
    if (result?.alerts) localStorage.setItem("finsight_last_alerts",JSON.stringify(result.alerts));
    localStorage.setItem("finsight_robot_alert_upload_id", String(uploadId));
    localStorage.removeItem("finsight_show_robot_alert");
    completed=true; clearInterval(animation); paint(steps.length); view.disabled=false; setAnalysisRunning(false, uploadId);
    status.textContent="Analysis completed successfully.";
  }catch(error){
    completed=true; clearInterval(animation); setAnalysisRunning(false, uploadId); status.textContent=error.message; status.classList.add("error-text");
  }
  // "View report" opens report.html for this upload once the analysis has finished.
  view?.addEventListener("click",()=>{
    if(completed && !isAnalysisRunning()){
      localStorage.setItem("finsight_show_robot_alert", "true");
      localStorage.setItem("finsight_robot_alert_upload_id", String(uploadId));
      location.href=`report.html?uploadHistoryId=${uploadId}`;
    }
  });
});
