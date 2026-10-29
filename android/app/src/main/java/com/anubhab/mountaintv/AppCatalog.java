package com.anubhab.mountaintv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.List;

/** Known favourites with artwork and launch targets resolved from installed TV packages. */
final class AppCatalog {
    static final class App {
        final String id, name;
        final String[] packages;
        String installedPackage;
        Drawable artwork;
        App(String id, String name, String... packages) {
            this.id = id; this.name = name; this.packages = packages;
        }
        boolean installed() { return installedPackage != null; }
    }

    final List<App> apps = new ArrayList<>();
    AppCatalog() {
        apps.add(new App("youtube", "YouTube", "com.google.android.youtube.tv"));
        apps.add(new App("prime", "Prime Video", "com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"));
        apps.add(new App("sonyliv", "Sony LIV", "com.sonyliv", "com.sonyliv.androidtv"));
        apps.add(new App("youtubemusic", "YouTube Music", "com.google.android.youtube.tvmusic", "com.google.android.apps.youtube.music"));
        apps.add(new App("hotstar", "JioHotstar", "in.startv.hotstar"));
        apps.add(new App("appletv", "Apple TV", "com.apple.atve.androidtv.appletv", "com.apple.atve.sony.appletv"));
        apps.add(new App("netflix", "Netflix", "com.netflix.ninja", "com.netflix.mediaclient"));
        apps.add(new App("zee5", "ZEE5", "com.graymatrix.did"));
        apps.add(new App("spotify", "Spotify", "com.spotify.tv.android"));
        apps.add(new App("movies", "Google Play Movies", "com.google.android.videos"));
        apps.add(new App("livetv", "Live TV", "com.smartdevice.livetv"));
        apps.add(new App("playstore", "Google Play Store", "com.android.vending"));
    }

    void refresh(Context context) {
        PackageManager pm = context.getPackageManager();
        for (App app : apps) {
            app.installedPackage = null; app.artwork = null;
            for (String pkg : app.packages) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
                    if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
                    if (!info.enabled || launch == null) continue;
                    app.installedPackage = pkg;
                    android.content.pm.ResolveInfo activity = pm.resolveActivity(launch, 0);
                    if (activity != null) app.artwork = activity.activityInfo.loadBanner(pm);
                    if (app.artwork == null) app.artwork = pm.getApplicationBanner(info);
                    if (app.artwork == null) app.artwork = pm.getApplicationIcon(info);
                    break;
                } catch (PackageManager.NameNotFoundException ignored) { }
            }
        }
    }

    App find(String id) {
        for (App app : apps) if (app.id.equals(id)) return app;
        return null;
    }
}
