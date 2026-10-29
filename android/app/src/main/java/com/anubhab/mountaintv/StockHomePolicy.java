package com.anubhab.mountaintv;

import java.util.Locale;

/** Exclude this launcher and setup/recovery activities from the stock-home shortcut. */
final class StockHomePolicy {
    static boolean isCandidate(String ownPackage,String packageName,String activityName) {
        if(packageName == null || activityName == null || packageName.equals(ownPackage)) return false;
        String name=(packageName+"/"+activityName).toLowerCase(Locale.ROOT);
        return !name.contains("resolveractivity") && !name.contains("fallbackhome")
            && !name.contains("setupwizard") && !name.contains("provision")
            && !name.contains("factory") && !name.contains("testactivity")
            && !packageName.equals("android") && !packageName.equals("com.android.settings");
    }
}
