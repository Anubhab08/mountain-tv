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
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/** Opt-in Home redirection plus a one-shot native Settings menu shortcut.
 * Content lookup is restricted to TV Settings during the eight seconds after a Bluetooth click.
 * Only Home key events are consumed; all other keys pass through without inspection.
 * No gestures, screenshots, content logging or networking. */
public final class HomeHelperService extends AccessibilityService {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private static final String TV_SETTINGS="com.android.tv.settings";
    private static HomeHelperService connected;
    private long accessoriesUntil;
    private final Runnable accessoriesTimeout=this::cancelAccessoriesRequest;
    private final Runnable selectAccessories=this::trySelectAccessories;
    private ComponentName stockHome;
    private long lastLaunch;
    private final HomePressPolicy homePress=new HomePressPolicy();
    private boolean queued;
    private final Runnable redirect=new Runnable() {
        @Override public void run() {
            queued=false;
            if(HomeHelperState.suspended(HomeHelperService.this) || stockHome == null) return;
            launchMountainHome();
        }
    };
    private void launchMountainHome() {
        HomeHelperState.endSearch();
        lastLaunch=SystemClock.uptimeMillis();
        Intent home=new Intent(this,MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try { startActivity(home); }
        catch(android.content.ActivityNotFoundException | SecurityException ignored) { }
    }
    @Override protected boolean onKeyEvent(KeyEvent event) {
        if(event==null || event.getKeyCode()!=KeyEvent.KEYCODE_HOME) return false;
        if(HomeHelperState.suspended(this)) { homePress.cancel(); return false; }
        if(event.getAction()==KeyEvent.ACTION_DOWN) {
            homePress.down(event.getEventTime(),event.getRepeatCount()>0);
            return true;
        }
        if(event.getAction()!=KeyEvent.ACTION_UP) return false;
        HomePressPolicy.Action action=homePress.up(event.getEventTime(),event.isCanceled(),ViewConfiguration.getLongPressTimeout());
        cancelAccessoriesRequest(); handler.removeCallbacks(redirect); queued=false;
        if(action==HomePressPolicy.Action.HOME) launchMountainHome();
        else if(action==HomePressPolicy.Action.VOICE) {
            HomeHelperState.beginSearch();
            if(!GoogleSearchLauncher.open(this,true)) HomeHelperState.endSearch();
        }
        return true;
    }
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
        connected=this;
        updateEventFilter();
        if(stockHome != null) probeAtBoot();
    }
    // Same-process call: no exported command receiver or persistent automation request.
    static boolean prepareAccessories() {
        HomeHelperService service=connected;
        if(service==null || (service.getServiceInfo().getCapabilities()
                & AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT)==0) return false;
        service.accessoriesUntil=SystemClock.elapsedRealtime()+8000;
        service.handler.removeCallbacks(service.redirect); service.queued=false;
        service.updateEventFilter();
        service.handler.removeCallbacks(service.accessoriesTimeout);
        service.handler.postDelayed(service.accessoriesTimeout,8000);
        return true;
    }
    static void cancelAccessories() {
        if(connected!=null) connected.cancelAccessoriesRequest();
    }
    private void updateEventFilter() {
        AccessibilityServiceInfo info=getServiceInfo();
        if(info==null) return;
        boolean pending=accessoriesUntil>SystemClock.elapsedRealtime();
        info.eventTypes=pending ? AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            : stockHome==null ? 0 : AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.packageNames=pending ? new String[]{TV_SETTINGS}
            : stockHome==null ? new String[]{getPackageName()}
            : new String[]{stockHome.getPackageName(),"com.google.android.katniss"};
        info.flags=AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS; info.notificationTimeout=0;
        setServiceInfo(info);
    }
    private void cancelAccessoriesRequest() {
        if(accessoriesUntil==0) return;
        accessoriesUntil=0;
        handler.removeCallbacks(selectAccessories); handler.removeCallbacks(accessoriesTimeout);
        updateEventFilter();
    }
    private void trySelectAccessories() {
        if(accessoriesUntil<=SystemClock.elapsedRealtime()) { cancelAccessoriesRequest(); return; }
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null) return;
        try {
            if(!TV_SETTINGS.contentEquals(root.getPackageName()==null ? "" : root.getPackageName())) return;
            // Exact visible menu labels only; never select Add accessory, paired devices or toggles.
            for(String label:new String[]{"Remotes & Accessories","Remotes and accessories"}) {
                java.util.List<AccessibilityNodeInfo> matches=root.findAccessibilityNodeInfosByText(label);
                try {
                    for(AccessibilityNodeInfo match:matches) {
                        if(match.getText()==null || !label.equalsIgnoreCase(match.getText().toString().trim())
                                || !match.isVisibleToUser()) continue;
                        AccessibilityNodeInfo target=AccessibilityNodeInfo.obtain(match);
                        try {
                            for(int depth=0;target!=null && depth<5;depth++) {
                                if(!TV_SETTINGS.contentEquals(target.getPackageName()==null ? "" : target.getPackageName())) break;
                                if(target.isClickable() && target.isEnabled()) {
                                    // Consume before clicking so the next window can never trigger another action.
                                    cancelAccessoriesRequest();
                                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    return;
                                }
                                AccessibilityNodeInfo parent=target.getParent(); target.recycle(); target=parent;
                            }
                        } finally { if(target!=null) target.recycle(); }
                    }
                } finally { for(AccessibilityNodeInfo match:matches) match.recycle(); }
            }
        } finally { root.recycle(); }
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
        if(event==null) return;
        if(accessoriesUntil!=0) {
            if(TV_SETTINGS.contentEquals(event.getPackageName()==null ? "" : event.getPackageName())) {
                handler.removeCallbacks(selectAccessories); handler.postDelayed(selectAccessories,120);
            }
            return;
        }
        if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || stockHome == null) return;
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
    @Override public void onInterrupt() { homePress.cancel(); cancelAccessoriesRequest(); handler.removeCallbacks(redirect); queued=false; }
    @Override public void onDestroy() { if(connected==this) connected=null; accessoriesUntil=0; handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}
