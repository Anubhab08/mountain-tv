/* 60% continuous corner geometry adapted from figma-squircle (MIT, Tien Pham).
   See ../assets/FIGMA-SQUIRCLE-LICENSE.txt. Paths update only when layout changes. */
"use strict";
function smoothPath(w,h,r){
  const s=.6, pMax=Math.min(w,h)/2;
  r=Math.min(r,pMax/(1+s));
  const p=(1+s)*r, arc=Math.sin(Math.PI/4*(1-s))*r*Math.SQRT2;
  const beta=Math.PI/4*s, c=r*Math.tan(beta/2)*Math.cos(beta), d=c*Math.tan(beta);
  const b=(p-arc-c-d)/3,a=2*b;
  return `M ${w-p} 0 c ${a} 0 ${a+b} 0 ${a+b+c} ${d} a ${r} ${r} 0 0 1 ${arc} ${arc} c ${d} ${c} ${d} ${b+c} ${d} ${a+b+c}
L ${w} ${h-p} c 0 ${a} 0 ${a+b} ${-d} ${a+b+c} a ${r} ${r} 0 0 1 ${-arc} ${arc} c ${-c} ${d} ${-b-c} ${d} ${-a-b-c} ${d}
L ${p} ${h} c ${-a} 0 ${-a-b} 0 ${-a-b-c} ${-d} a ${r} ${r} 0 0 1 ${-arc} ${-arc} c ${-d} ${-c} ${-d} ${-b-c} ${-d} ${-a-b-c}
L 0 ${p} c 0 ${-a} 0 ${-a-b} ${d} ${-a-b-c} a ${r} ${r} 0 0 1 ${arc} ${-arc} c ${c} ${-d} ${b+c} ${-d} ${a+b+c} ${-d} Z`.replace(/\s+/g," ");
}
const corners = new ResizeObserver(entries=>{
  for(const {target} of entries){
    const w=target.offsetWidth,h=target.offsetHeight;
    if(!w||!h)continue;
    const r=parseFloat(getComputedStyle(target).borderTopLeftRadius)||12;
    target.style.setProperty("--smooth-path",`path('${smoothPath(w,h,r)}')`);
  }
});
document.querySelectorAll(".dock,.app-card,.panel,.search-field,.service-button,.primary-button,.search-submit,.toast").forEach(el=>corners.observe(el));
// Service choices register with this observer when app.js creates them.
