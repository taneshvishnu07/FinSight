/* FinSight File Notes: Handles password changes from the Profile and Settings page. */
document.addEventListener("DOMContentLoaded",()=>{
  const form=qs("#passwordForm"); if(!form)return;
  form.addEventListener("submit",async event=>{
    event.preventDefault();
    if(!form.checkValidity()){form.reportValidity();return;}
    const currentPassword=qs("#currentPassword").value;
    const newPassword=qs("#newPassword").value;
    const confirmPassword=qs("#confirmPassword").value;
    const message=qs("#passwordMessage");
    if(newPassword!==confirmPassword){setMessage(message,"New password and confirmation do not match.");return;}
    try{
      setMessage(message,"Changing password...");
      await API.changePassword({currentPassword,newPassword,confirmPassword});
      form.reset();
      setMessage(message,"Password changed successfully.",true);
    }catch(error){setMessage(message,error.message||"Unable to change password.");}
  });
});
