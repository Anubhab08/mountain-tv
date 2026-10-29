package com.anubhab.mountaintv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/** Opt-in Home redirection for firmware that gives its system launcher higher priority.
 * Receives only window-state events from the chosen launcher and Google search. No window-content
 * retrieval, key filtering, gestures, screenshots, text inspection, logging or networking. */
public final class HomeHelperService extends AccessibilityService {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ComponentName stockHome;
    private long lastLaunch;
    private boolean queued;
    private final Runnable redirect=new Runnable() {
        @Override public void run() {
            queued=false;
            if(HomeHelperState.suspended(HomeHelperService.this) || stockHome == null) return;
            HomeHelperState.endSearch();
            lastLaunch=SystemClock.uptimeMillis();
            // An explicit ordinary-app intent keeps the stock system home task intact.
            Intent home=new Intent(HomeHelperService.this,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try { startActivity(home); }
            catch(android.content.ActivityNotFoundException | SecurityException ignored) { }
        }
    };
    @Override protected void onServiceConnected() {
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        PackageManager pm=getPackageManager();
        ResolveInfo current=pm.resolveActivity(home,PackageManager.MATCH_DEFAULT_ONLY);
        String saved=getSharedPreferences("search",MODE_PRIVATE).getString("stock_home","");
        java.util.List<ComponentName> candidates=new java.util.ArrayList<>();
        for(ResolveInfo match:pm.queryIntentActivities(home,PackageManager.MATCH_DEFAULT_ONLY)) {
            if(match.activityInfo == null || !match.activityInfo.exported || !match.activityInfo.enabled
                    || !match.activityInfo.applicationInfo.enabled) continue;
            if(!StockHomePolicy.isCandidate(getPackageName(),match.activityInfo.packageName,match.activityInfo.name)) continue;
            ComponentName component=new ComponentName(match.activityInfo.packageName,match.activityInfo.name);
            candidates.add(component);
            if(component.flattenToString().equals(saved)) stockHome=component;
        }
        if(stockHome == null && current != null && current.activityInfo != null) {
            ComponentName preferred=new ComponentName(current.activityInfo.packageName,current.activityInfo.name);
            if(candidates.contains(preferred)) stockHome=preferred;
        }
        if(stockHome == null && candidates.size()==1) stockHome=candidates.get(0);
        AccessibilityServiceInfo info=getServiceInfo();
        if(info == null) return;
        info.eventTypes=stockHome == null ? 0 : AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.packageNames=stockHome == null ? new String[]{getPackageName()}
            : new String[]{stockHome.getPackageName(),"com.google.android.katniss"};
        info.notificationTimeout=0;
        info.flags=0; // No key filtering, interactive-window retrieval or other capabilities.
        setServiceInfo(info);
        if(stockHome != null) probeAtBoot();
    }
    private void probeAtBoot() {
        int boot=android.provider.Settings.Global.getInt(getContentResolver(),android.provider.Settings.Global.BOOT_COUNT,-1);
        android.content.SharedPreferences state=getSharedPreferences("home_helper",MODE_PRIVATE);
        if(!HomeHelperPolicy.shouldProbeAtBoot(boot,state.getInt("probed_boot",-1),SystemClock.elapsedRealtime(),HomeHelperState.suspended(this))) return;
        state.edit().putInt("probed_boot",boot).apply();
        try {
            startActivity(new Intent(this,HomeProbeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch(android.content.ActivityNotFoundException | SecurityException ignored) { }
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event == null || event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || stockHome == null) return;
        boolean isHome=HomeHelperPolicy.shouldRedirect(stockHome.getPackageName(),stockHome.getClassName(),
            event.getPackageName(),event.getClassName(),false);
        if(!isHome) {
            // Google may pass through MainActivity on its way to input or a title page.
            // Ordinary widget events from Home must not cancel its pending redirect.
            if(isSearchWindow(event)) {
                handler.removeCallbacks(redirect); queued=false;
            }
            return;
        }
        if(HomeHelperState.suspended(this) || queued) return;
        queued=true;
        handler.postDelayed(redirect,HomeHelperState.searching() ? 1500 : Math.max(0,200-(SystemClock.uptimeMillis()-lastLaunch)));
    }
    private boolean isSearchWindow(AccessibilityEvent event) {
        if(event.getPackageName()==null || event.getClassName()==null) return false;
        String pkg=event.getPackageName().toString();
        if("com.google.android.katniss".equals(pkg)) return HomeHelperState.searching();
        if(!stockHome.getPackageName().equals(pkg)) return false;
        String name=event.getClassName().toString();
        if(name.startsWith(".")) name=pkg+name;
        try {
            getPackageManager().getActivityInfo(new ComponentName(pkg,name),0);
            HomeHelperState.endSearch();
            return true;
        } catch(PackageManager.NameNotFoundException ignored) { return false; }
    }
    @Override public void onInterrupt() { handler.removeCallbacks(redirect); queued=false; }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}
