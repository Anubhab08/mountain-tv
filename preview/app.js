"use strict";

const apps = [
  {id:"youtube", name:"YouTube"},
  {id:"prime", name:"Prime Video"},
  {id:"sonyliv", name:"Sony LIV"},
  {id:"music", name:"YouTube Music"},
  {id:"hotstar", name:"JioHotstar"},
  {id:"apple", name:"Apple TV"},
  {id:"netflix",name:"Netflix"}, {id:"zee5",name:"ZEE5"},
  {id:"spotify",name:"Spotify"}, {id:"movies",name:"Google Play Movies"},
  {id:"livetv",name:"Live TV"}, {id:"playstore",name:"Google Play Store"}
];
const wifiDialog = document.querySelector("#wifi-dialog");
const toastElement = document.querySelector("#toast");
const homeApps = [...document.querySelectorAll(".app")];
const utilityButtons = [...document.querySelectorAll(".utility")];
let toastTimeout;
let lastApp = 0;

function updateClock() {
  const parts = new Intl.DateTimeFormat("en-IN", {hour:"numeric",minute:"2-digit",hour12:true}).formatToParts(new Date());
  const time = parts.filter(part=>["hour","minute","literal"].includes(part.type)).map(part=>part.value).join("").trim();
  const period = parts.find(part=>part.type === "dayPeriod")?.value.toUpperCase() || "";
  document.querySelector("#clock").replaceChildren(document.createTextNode(time));
  const small = document.createElement("small");
  small.textContent = period;
  document.querySelector("#clock").append(small);
}
updateClock();
setInterval(updateClock, 30000);

function toast(message) {
  clearTimeout(toastTimeout);
  document.querySelector("#toast-message").textContent = message;
  toastElement.classList.add("show");
  toastTimeout = setTimeout(()=>toastElement.classList.remove("show"), 4800);
}

function openDialog(dialog) {
  toastElement.classList.remove("show");
  dialog.showModal();
  dialog.querySelector(".close-dialog").focus();
}

function openGoogleSearch(){toast("On TV, Search opens Google’s existing search for speaking or typing. Google shows the streaming results. This browser preview cannot open TV apps.");}
document.querySelector("#search-button").addEventListener("click",openGoogleSearch);
document.querySelector("#stock-home-button").addEventListener("click",()=>toast("On TV, this dot opens the original home and pauses the Home helper. Open Mountain TV from Apps to resume. This preview cannot switch TV launchers."));
document.querySelector("#bluetooth-button").addEventListener("click",()=>toast("On TV, this opens Remotes & Accessories. Bluetooth settings cannot open in this browser preview."));
document.querySelector("#wifi-button").addEventListener("click",()=>openDialog(wifiDialog));
document.querySelectorAll(".close-dialog").forEach(button=>button.addEventListener("click",()=>button.closest("dialog").close()));
document.querySelectorAll("dialog").forEach(dialog=>dialog.addEventListener("click",event=>{
  if(event.target !== dialog) return;
  const rect = dialog.getBoundingClientRect();
  if(event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) dialog.close();
}));

homeApps.forEach((button,index)=>{
  button.addEventListener("focus",()=>{lastApp=index;});
  button.addEventListener("click",()=>{
    const app = apps.find(item=>item.id===button.dataset.app);
    toast(`${app.name} opens on your TV. This local preview cannot launch TV apps.`);
  });
});

document.querySelector("#open-wifi-settings").addEventListener("click",()=>{
  wifiDialog.close();
  toast("On your TV, this shortcut opens Network & Internet settings.");
});

document.addEventListener("keydown",event=>{
  if(wifiDialog.open) return;
  const active=document.activeElement;
  if(event.key==="/" || event.key.toLowerCase()==="s"){
    event.preventDefault();openGoogleSearch();return;
  }
  if(active.dataset?.row==="apps"){
    const index=homeApps.indexOf(active);
    const columns=window.matchMedia("(max-width:600px)").matches?3:6;
    let next=index;
    if(event.key==="ArrowLeft") next=Math.max(0,index-1);
    else if(event.key==="ArrowRight") next=Math.min(homeApps.length-1,index+1);
    else if(event.key==="ArrowUp"){
      if(index<columns){event.preventDefault();utilityButtons[0].focus();return;}
      next=index-columns;
    }else if(event.key==="ArrowDown") next=Math.min(homeApps.length-1,index+columns);
    else return;
    event.preventDefault();homeApps[next].focus();
  }else if(active.dataset?.row==="top"){
    const index=utilityButtons.indexOf(active);
    if(event.key==="ArrowDown"){event.preventDefault();homeApps[lastApp].focus();}
    else if(event.key==="ArrowRight"){event.preventDefault();utilityButtons[Math.min(index+1,utilityButtons.length-1)].focus();}
    else if(event.key==="ArrowLeft"){event.preventDefault();utilityButtons[Math.max(index-1,0)].focus();}
  }else if(["ArrowLeft","ArrowRight","ArrowUp","ArrowDown"].includes(event.key)){
    event.preventDefault();homeApps[lastApp].focus();
  }
});

// Match the default focused tile of the TV launcher on first load.
homeApps[0].focus({preventScroll:true});
