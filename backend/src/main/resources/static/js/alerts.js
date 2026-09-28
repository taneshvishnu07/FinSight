/* FinSight File Notes: Loads alerts and controls the persistent alert popup shown across the application. */
window.FinSightAlerts={
 getDismissed(){try{return new Set(JSON.parse(localStorage.getItem("finsight_dismissed_alert_ids")||"[]").map(String));}catch{return new Set();}},
 saveDismissed(s){localStorage.setItem("finsight_dismissed_alert_ids",JSON.stringify([...s].slice(-300)));},
 getDismissedAnalyses(){try{return new Set(JSON.parse(localStorage.getItem("finsight_dismissed_robot_analyses")||"[]").map(String));}catch{return new Set();}},
 saveDismissedAnalysis(id){if(id==null)return;const s=this.getDismissedAnalyses();s.add(String(id));localStorage.setItem("finsight_dismissed_robot_analyses",JSON.stringify([...s].slice(-100)));},
 async get(uploadId){
   if(isAnalysisRunning())return {alerts:[],unreadCount:0};
   const id=uploadId||await resolveLatestUploadId();
   if(!id)return {alerts:[],unreadCount:0};
   return API.getAlerts(id);
 },
 async showOnDashboard(){
   const appPages=["dashboard.html","transactions.html","report.html","subscriptions.html","unusual-spending.html","recommendations.html","alerts.html"];
   const currentPage=location.pathname.split("/").pop()||"dashboard.html";
   if(!appPages.includes(currentPage))return;

   const mount=qs("#robotAlertMount");
   if(!mount)return;

   // The popup is tied to the latest completed analysis. A new analysis starts
   // a new popup lifecycle; X, Later, and View alerts dismiss it for that analysis.
   const requestedUploadId=Number(localStorage.getItem("finsight_robot_alert_upload_id")||latestUploadIdFromStorage());
   if(!requestedUploadId)return;

   let data=null;
   try{
     data=await this.get(requestedUploadId);
   }catch(e){
     console.warn("Alert popup",e);
     return;
   }

   try{
     const dismissed=this.getDismissed();
     const lastAnalysis=(()=>{try{return JSON.parse(localStorage.getItem("finsight_last_analysis")||"null");}catch{return null;}})();
     const analysisId=data?.analysisId ?? lastAnalysis?.analysisId;
     const allItems=Array.isArray(data?.alerts)?data.alerts:[];
     const items=allItems.filter(x=>x && String(x.type||"").toUpperCase()!=="LARGE_TRANSACTION" && x.id!=null && !dismissed.has(String(x.id)));
     if(!items.length)return;
     if(analysisId!=null && this.getDismissedAnalyses().has(String(analysisId)))return;

     const count=items.length;
     mount.innerHTML=`<aside class="robot-alert" role="dialog" aria-live="polite" aria-label="FinSight financial alerts">
       <button class="robot-close" id="closeRobot" aria-label="Close alert popup">×</button>
       <span class="robot-avatar" aria-hidden="true"><i class="bi bi-exclamation-triangle-fill"></i></span>
       <div class="robot-copy">
         <h3>You have ${count} alert${count===1?"":"s"}</h3>
         <p>FinSight found ${count===1?"something":"some items"} worth checking in your latest analysis.</p>
         <div class="robot-actions"><a href="alerts.html" id="viewRobotAlerts">View alerts</a><button id="dismissRobot" type="button">Later</button></div>
       </div>
     </aside>`;

     const dismiss=()=>{
       items.forEach(x=>dismissed.add(String(x.id)));
       this.saveDismissed(dismissed);
       this.saveDismissedAnalysis(analysisId);
       localStorage.removeItem("finsight_show_robot_alert");
       mount.replaceChildren();
     };
     qs("#closeRobot")?.addEventListener("click",dismiss);
     qs("#dismissRobot")?.addEventListener("click",dismiss);
     qs("#viewRobotAlerts")?.addEventListener("click",()=>dismiss());
   }catch(e){console.warn("Alert popup",e);}
 },
 async initPage(){
   const mount=qs("#alertPageList");
   if(!mount)return;
   if(isAnalysisRunning()){showAnalysisPendingPage();return;}
   try{
     const id=await resolveLatestUploadId();
     if(!id){mount.innerHTML="<p class='muted'>Upload transactions to start receiving alerts.</p>";return;}
     const data=await API.getAlerts(id),list=(data.alerts||[]).filter(x=>String(x.type||"").toUpperCase()!=="LARGE_TRANSACTION");
     qs("#readAllAlerts")?.classList.toggle("hidden",list.every(x=>x.isRead));
     mount.innerHTML=list.map(x=>`<div class="alert-item ${x.isRead?"read":""}"><div class="left"><div class="item-icon"><i class="bi bi-bell"></i></div><div><h3>${esc(x.title||x.type||"Financial alert")}</h3><p>${esc(x.message||"")}</p><small class="muted">${esc(formatDateTime(x.createdAt))}</small></div></div><div class="item-value"><small class="severity ${String(x.severity||"LOW").toLowerCase()}">${esc(x.severity||"LOW")}</small>${!x.isRead?`<button class="btn btn-light mark-read" data-alert="${esc(x.id)}">Mark read</button>`:""}</div></div>`).join("")||"<p class='muted'>You're all clear. No active alerts.</p>";
     qsa(".mark-read").forEach(b=>b.addEventListener("click",async()=>{try{await API.markAlertRead(id,Number(b.dataset.alert));await this.initPage();}catch(e){alert(e.message);}}));
     qs("#readAllAlerts")?.addEventListener("click",async()=>{try{await API.markAllAlertsRead(id);await this.initPage();}catch(e){alert(e.message);}});
   }catch(e){mount.innerHTML=`<p class='muted'>${esc(e.message)}</p>`;}
 }
};
document.addEventListener("DOMContentLoaded",()=>{
  window.FinSightAlerts?.initPage();
  window.FinSightAlerts?.showOnDashboard();
});
