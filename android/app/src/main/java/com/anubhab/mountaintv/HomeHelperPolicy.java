package com.anubhab.mountaintv;

/** Exact activity matching avoids intercepting search/details/settings within the stock app. */
final class HomeHelperPolicy {
    static boolean shouldProbeAtBoot(int boot,int previousBoot,long uptime,boolean suspended) {
        return !suspended && boot>=0 && boot!=previousBoot && uptime>=0 && uptime<180_000;
    }
    static boolean shouldRedirect(String stockPackage,String stockActivity,CharSequence eventPackage,CharSequence eventActivity,boolean suspended) {
        if(suspended || stockPackage == null || stockActivity == null || eventPackage == null || eventActivity == null) return false;
        if(!stockPackage.contentEquals(eventPackage)) return false;
        String name=eventActivity.toString();
        if(name.startsWith(".")) name=stockPackage+name;
        return stockActivity.equals(name);
    }
}
