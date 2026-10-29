package com.anubhab.mountaintv;

import android.content.Context;

final class HomeHelperState {
    private static long searchUntil;
    static void beginSearch() { searchUntil=android.os.SystemClock.elapsedRealtime()+600_000; }
    static boolean searching() { return android.os.SystemClock.elapsedRealtime()<searchUntil; }
    static void endSearch() { searchUntil=0; }

    static boolean suspended(Context context) {
        return context.getSharedPreferences("home_helper",Context.MODE_PRIVATE).getBoolean("suspended",false);
    }
    static void suspend(Context context) {
        context.getSharedPreferences("home_helper",Context.MODE_PRIVATE).edit().putBoolean("suspended",true).apply();
    }
    static void resume(Context context) {
        context.getSharedPreferences("home_helper",Context.MODE_PRIVATE).edit().putBoolean("suspended",false).apply();
    }
}
