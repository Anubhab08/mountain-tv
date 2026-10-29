package com.anubhab.mountaintv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;

/** The same ASSIST intent and input-mode extra used by this TV's Google launcher.
 * Google owns input, queries, availability results and provider launching. */
final class GoogleSearchLauncher {
    static boolean open(Activity activity,boolean voice) {
        Intent search=new Intent(Intent.ACTION_ASSIST).setPackage("com.google.android.katniss")
            .putExtra("search_type",voice ? 1 : 2)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        ResolveInfo target=activity.getPackageManager().resolveActivity(search,0);
        if(target == null || target.activityInfo == null || !target.activityInfo.exported
                || !target.activityInfo.enabled || !target.activityInfo.applicationInfo.enabled) return false;
        try { activity.startActivity(search); return true; }
        catch(ActivityNotFoundException | SecurityException ignored) { return false; }
    }
}
