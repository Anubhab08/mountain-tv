package com.anubhab.mountaintv;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final AppCatalog catalog = new AppCatalog();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService effects = Executors.newSingleThreadExecutor();
    private Typeface font, mediumFont;
    private SharedPreferences prefs;
    private TextView clock;
    private View wifi, bluetooth, searchButton;
    private View firstTile, stockHomeButton;
    private LinearLayout dock;
    private FrameLayout scene;
    private ImageView softenedBackdrop;
    private View dockGlass;
    private Bitmap wallpaperBitmap, glassBitmap;
    private int openPanels;
    private int backdropGeneration;
    private boolean active;
    private String focusedAppId = "youtube";
    private final Runnable searchReturned = () -> {
        if(getWindow().getDecorView().hasWindowFocus()) HomeHelperState.endSearch();
    };
    private final Runnable clockTick = new Runnable() {
        @Override public void run() { updateStatus(); if (active) handler.postDelayed(this, 60000); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("search", MODE_PRIVATE);
        prefs.edit().remove("endpoint").remove("token").remove("country").apply();
        font = Typeface.createFromAsset(getAssets(), "GoogleSans-Regular.ttf");
        mediumFont = Typeface.createFromAsset(getAssets(), "GoogleSans-Medium.ttf");
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        buildHome();
    }

    private void buildHome() {
        scene = new FrameLayout(this);
        scene.setBackgroundColor(Color.rgb(16, 29, 36));
        ImageView wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try (java.io.InputStream input = getAssets().open("mountains.jpg")) {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
            wallpaperBitmap = android.graphics.BitmapFactory.decodeStream(input, null, options);
            wallpaper.setImageBitmap(wallpaperBitmap);
        } catch (java.io.IOException ignored) { }
        scene.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
        softenedBackdrop = new ImageView(this);
        softenedBackdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        softenedBackdrop.setAlpha(0f);
        scene.addView(softenedBackdrop, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x3A101B24,0x0A101B24,0x99101B24}));
        scene.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(dp(54), dp(33), dp(54), dp(56));
        scene.addView(home, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout top = new FrameLayout(this);
        top.setClipChildren(false); top.setClipToPadding(false);
        LinearLayout search = row(); search.setGravity(Gravity.CENTER);
        search.setId(View.generateViewId()); search.setFocusable(true); search.setClickable(true);
        search.setContentDescription("Google search");
        search.setBackground(new FrostedGlassDrawable(search,scene,null,0x24EDF3F7,0,true));
        ImageView searchIcon = new ImageView(this); searchIcon.setImageDrawable(new Symbol("search"));
        search.addView(searchIcon,new LinearLayout.LayoutParams(dp(18),dp(18)));
        TextView searchLabel = text("Search",12,Color.WHITE); searchLabel.setTypeface(mediumFont);
        LinearLayout.LayoutParams searchLabelParams = new LinearLayout.LayoutParams(-2,-2); searchLabelParams.leftMargin=dp(10);
        search.addView(searchLabel,searchLabelParams);
        search.setOnFocusChangeListener((v,f) -> animateFocus(v,f,1.025f,22));
        search.setOnClickListener(v -> openGoogleSearch());
        search.setOnLongClickListener(v -> { openGoogleSearch(true); return true; });
        searchButton=search;
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(dp(224),dp(42),Gravity.CENTER);
        top.addView(searchButton,sp);
        LinearLayout status = row();
        stockHomeButton = statusControl("dot","Open original TV home",this::openStockHome);
        status.addView(stockHomeButton,new LinearLayout.LayoutParams(dp(32),dp(42)));
        wifi = statusControl("wifi","Wi-Fi settings",this::openWifi);
        status.addView(wifi,new LinearLayout.LayoutParams(dp(34),dp(42)));
        bluetooth = statusControl("bluetooth","Remotes & accessories",this::openAccessories);
        status.addView(bluetooth,new LinearLayout.LayoutParams(dp(34),dp(42)));
        clock = text("",14,Color.WHITE); clock.setPadding(dp(12),0,0,0);
        status.addView(clock,new LinearLayout.LayoutParams(-2,dp(42)));
        top.addView(status,new FrameLayout.LayoutParams(-2,dp(42),Gravity.END|Gravity.CENTER_VERTICAL));
        home.addView(top,new LinearLayout.LayoutParams(-1,dp(42)));

        home.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));
        FrameLayout shelf = new FrameLayout(this);
        shelf.setClipChildren(false); shelf.setClipToPadding(false);
        dockGlass = shelf;
        dockGlass.setBackground(new FrostedGlassDrawable(dockGlass,scene,null,0x48EDF3F7,dp(22)));
        dock = new LinearLayout(this); dock.setOrientation(LinearLayout.VERTICAL);
        dock.setClipChildren(false); dock.setClipToPadding(false);
        dock.setPadding(dp(14),dp(14),dp(14),dp(14));
        shelf.addView(dock,new FrameLayout.LayoutParams(-1,-2));
        home.setClipChildren(false); home.setClipToPadding(false);
        home.addView(shelf,new LinearLayout.LayoutParams(-1,-2));
        setContentView(scene);
        if (wallpaperBitmap != null) effects.execute(() -> {
            Bitmap blurred = FrostedGlassDrawable.blurWallpaper(wallpaperBitmap);
            handler.post(() -> {
                if (isDestroyed()) return;
                glassBitmap = blurred;
                softenedBackdrop.setImageBitmap(blurred);
                dockGlass.setBackground(new FrostedGlassDrawable(dockGlass,scene,glassBitmap,0x48EDF3F7,dp(22)));
                searchButton.setBackground(new FrostedGlassDrawable(searchButton,scene,glassBitmap,
                    searchButton.hasFocus() ? 0x42EDF3F7 : 0x24EDF3F7,0,true));
            });
        });
    }

    private void refreshApps() {
        catalog.refresh(this); dock.removeAllViews(); firstTile=null;
        List<View> tiles=new ArrayList<>();
        View focusedTile=null;
        LinearLayout appRow=null;
        final int columns=6;
        for(AppCatalog.App app:catalog.apps) {
            if(!app.installed()) continue;
            int index=tiles.size();
            if(index%columns==0) {
                appRow=row(); appRow.setClipChildren(false); appRow.setClipToPadding(false);
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
                if(index>0) rp.topMargin=dp(12);
                dock.addView(appRow,rp);
            }
            AspectCard card=new AspectCard();
            card.setId(View.generateViewId()); card.setFocusable(true); card.setClickable(true);
            card.setContentDescription(app.name); card.setBackground(appSurface(app.id));
            card.setClipToOutline(true); card.setElevation(dp(3));
            ImageView art=new ImageView(this); art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            art.setImageDrawable(appMark(app));
            boolean originalBanner=app.id.equals("netflix") || app.id.equals("zee5") || app.id.equals("spotify")
                || app.id.equals("movies") || app.id.equals("livetv") || app.id.equals("playstore");
            int insetX=originalBanner ? 0 : app.id.equals("prime") ? 13 : app.id.equals("appletv") ? 24 : 22;
            int insetY=originalBanner ? 0 : app.id.equals("sonyliv") ? 10 : app.id.equals("hotstar") ? 8 : 15;
            art.setPadding(dp(insetX),dp(insetY),dp(insetX),dp(insetY));
            card.addView(art,new FrameLayout.LayoutParams(-1,-1));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);
            cp.leftMargin=dp(5); cp.rightMargin=dp(5); appRow.addView(card,cp);
            tiles.add(card);
            card.setOnClickListener(v -> launchApp(app));
            card.setOnFocusChangeListener((v,focused) -> {
                if(focused) focusedAppId=app.id;
                card.animate().cancel();
                card.animate().scaleX(focused ? 1.07f : 1f).scaleY(focused ? 1.07f : 1f)
                    .translationY(focused ? -dp(3) : 0).translationZ(focused ? dp(10) : 0)
                    .setDuration(android.animation.ValueAnimator.areAnimatorsEnabled() ? 240 : 0)
                    .setInterpolator(new PathInterpolator(.16f,.86f,.22f,1f)).start();
            });
            if(firstTile==null) firstTile=card;
            if(app.id.equals(focusedAppId)) focusedTile=card;
        }
        if(firstTile==null) return;
        for(int i=0;i<tiles.size();i++) {
            View tile=tiles.get(i);
            tile.setNextFocusLeftId(tiles.get(i%columns==0 ? i : i-1).getId());
            tile.setNextFocusRightId(tiles.get(i%columns==columns-1 || i==tiles.size()-1 ? i : i+1).getId());
            tile.setNextFocusUpId(i<columns ? searchButton.getId() : tiles.get(i-columns).getId());
            tile.setNextFocusDownId(tiles.get(i+columns<tiles.size() ? i+columns : i).getId());
        }
        searchButton.setNextFocusDownId(firstTile.getId()); wifi.setNextFocusDownId(firstTile.getId()); stockHomeButton.setNextFocusDownId(firstTile.getId());
        searchButton.setNextFocusRightId(stockHomeButton.getId()); stockHomeButton.setNextFocusLeftId(searchButton.getId());
        stockHomeButton.setNextFocusRightId(wifi.getId()); wifi.setNextFocusLeftId(stockHomeButton.getId());
        wifi.setNextFocusRightId(bluetooth.getId()); bluetooth.setNextFocusLeftId(wifi.getId());
        bluetooth.setNextFocusRightId(bluetooth.getId()); bluetooth.setNextFocusDownId(firstTile.getId());
        View target=focusedTile==null ? firstTile : focusedTile;
        target.post(target::requestFocus);
    }

    /** The TV's density must not stretch artwork into a different card aspect ratio. */
    private final class AspectCard extends FrameLayout {
        private final Path artworkClip=new Path();
        AspectCard() { super(MainActivity.this); }
        @Override protected void onMeasure(int widthSpec,int heightSpec) {
            int width=View.MeasureSpec.getSize(widthSpec);
            super.onMeasure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(Math.round(width/1.65f),View.MeasureSpec.EXACTLY));
        }
        @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight) {
            super.onSizeChanged(width,height,oldWidth,oldHeight);
            SmoothSurfaceDrawable.continuousPath(artworkClip,new android.graphics.RectF(0,0,width,height),dp(12));
        }
        @Override protected void dispatchDraw(Canvas canvas) {
            int saved=canvas.save(); canvas.clipPath(artworkClip);
            super.dispatchDraw(canvas); canvas.restoreToCount(saved);
        }
    }

    private void animateFocus(View v, boolean focused, float scale, int radius) {
        if(v==searchButton) {
            v.setBackground(new FrostedGlassDrawable(v,scene,glassBitmap,
                focused ? 0x42EDF3F7 : 0x24EDF3F7,0,true));
        } else {
            v.setBackground(shape(focused ? 0x42FFFFFF : 0x20FFFFFF, Color.TRANSPARENT, radius));
        }
        boolean motion = android.animation.ValueAnimator.areAnimatorsEnabled();
        v.animate().cancel();
        v.animate().scaleX(focused ? scale : 1f).scaleY(focused ? scale : 1f)
            .setDuration(motion ? 240 : 0).setInterpolator(new PathInterpolator(.16f,.86f,.22f,1f)).start();
        v.setElevation(focused ? dp(7) : 0);
    }

    private Drawable appSurface(String id) {
        if (id.equals("hotstar")) {
            return new SmoothSurfaceDrawable(new int[]{0xFF057AFF,0xFF7835F5,0xFFEB08BD},dp(12));
        }
        int color;
        switch (id) {
            case "youtube": color = 0xFFF20B18; break;
            case "prime": color = 0xFF0578FF; break;
            case "youtubemusic": color = 0xFFFC123D; break;
            case "sonyliv": color = 0xFF08070C; break;
            case "netflix": case "movies": case "playstore": color = Color.WHITE; break;
            case "spotify": color = Color.BLACK; break;
            case "livetv": color = 0xFF484073; break;
            default: color = 0xFF101010; break;
        }
        return shape(color,Color.TRANSPARENT,12);
    }

    private Drawable appMark(AppCatalog.App app) {
        switch (app.id) {
            case "youtube": return getDrawable(R.drawable.brand_youtube_play);
            case "prime": return getDrawable(R.drawable.brand_prime);
            case "sonyliv": return getDrawable(R.drawable.brand_sonyliv);
            case "youtubemusic": return getDrawable(R.drawable.brand_youtubemusic);
            case "hotstar": return whiteHotstarMark();
            case "appletv": return getDrawable(R.drawable.brand_appletv);
            default: return app.artwork;
        }
    }

    private Drawable hotstarMark;
    private Drawable whiteHotstarMark() {
        if (hotstarMark != null) return hotstarMark;
        Bitmap source = android.graphics.BitmapFactory.decodeResource(getResources(),R.drawable.brand_hotstar);
        int[] pixels = new int[source.getWidth()*source.getHeight()];
        source.getPixels(pixels,0,source.getWidth(),0,0,source.getWidth(),source.getHeight());
        for (int i=0;i<pixels.length;i++) {
            int pixel = pixels[i];
            int light = Math.min(Color.red(pixel),Math.min(Color.green(pixel),Color.blue(pixel)));
            int alpha = Math.max(0,Math.min(255,(light-160)*255/80))*Color.alpha(pixel)/255;
            pixels[i] = Color.argb(alpha,255,255,255);
        }
        Bitmap mark = Bitmap.createBitmap(pixels,source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888);
        source.recycle();
        hotstarMark = new android.graphics.drawable.BitmapDrawable(getResources(),mark);
        return hotstarMark;
    }

    private void animateBackdrop() {
        if (softenedBackdrop == null) return;
        if (openPanels == 1 && !effects.isShutdown() && scene.getWidth() > 0 && scene.getHeight() > 0) {
            int request = ++backdropGeneration;
            int width = Math.min(320,scene.getWidth());
            int height = Math.max(1,Math.round(scene.getHeight()*width/(float)scene.getWidth()));
            Bitmap snapshot = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            Canvas capture = new Canvas(snapshot);
            capture.scale(width/(float)scene.getWidth(),height/(float)scene.getHeight());
            float previousAlpha = softenedBackdrop.getAlpha();
            softenedBackdrop.setAlpha(0f);
            scene.draw(capture);
            softenedBackdrop.setAlpha(previousAlpha);
            // Capture our own home once per sheet opening, never a continuous frame blur.
            effects.execute(() -> {
                Bitmap blurredHome = FrostedGlassDrawable.blurWallpaper(snapshot);
                snapshot.recycle();
                handler.post(() -> {
                    if (isDestroyed() || openPanels == 0 || request != backdropGeneration) return;
                    softenedBackdrop.setImageBitmap(blurredHome);
                    softenedBackdrop.bringToFront();
                    softenedBackdrop.animate().cancel();
                    softenedBackdrop.animate().alpha(1f)
                        .setDuration(android.animation.ValueAnimator.areAnimatorsEnabled() ? 240 : 0).start();
                });
            });
        }
        if (openPanels == 0) backdropGeneration++;
        softenedBackdrop.animate().cancel();
        softenedBackdrop.animate().alpha(openPanels > 0 ? 1f : 0f)
            .setDuration(android.animation.ValueAnimator.areAnimatorsEnabled() ? 240 : 0).start();
    }

    private void updateStatus() {
        String time=new SimpleDateFormat("h:mm a",Locale.getDefault()).format(new Date());
        android.text.SpannableString display=new android.text.SpannableString(time);
        int period=time.lastIndexOf(' ');
        if(period>=0) display.setSpan(new android.text.style.RelativeSizeSpan(.58f),period,time.length(),0);
        clock.setText(display);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkCapabilities network = cm.getNetworkCapabilities(cm.getActiveNetwork());
        String label = network == null ? "Offline" : network.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ? "Ethernet" : "Wi-Fi";
        wifi.setContentDescription(label + ", open network settings");
    }

    private void openWifi() {
        if (!startSafely(new Intent(Settings.ACTION_WIFI_SETTINGS))) {
            if (!startSafely(new Intent(Settings.ACTION_SETTINGS))) toast("Network settings aren't available on this TV.");
        }
    }

    private void openAccessories() {
        // Prefer a firmware-provided native accessories shortcut when available.
        if (startSafely(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) return;
        boolean helperReady=HomeHelperService.prepareAccessories();
        if (!startSafely(new Intent(Settings.ACTION_SETTINGS))) {
            HomeHelperService.cancelAccessories();
            toast("Remotes & accessories settings aren't available on this TV.");
        } else if(!helperReady) toast("Select Remotes & Accessories. Enable the Home helper in Accessibility for the direct shortcut.");
    }

    private View statusControl(String symbol,String description,Runnable action) {
        ImageView control=new ImageView(this); control.setImageDrawable(new Symbol(symbol));
        control.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        control.setId(View.generateViewId()); control.setFocusable(true); control.setClickable(true);
        control.setContentDescription(description); control.setPadding(dp(7),dp(11),dp(7),dp(11));
        control.setAlpha(.85f);
        control.setOnFocusChangeListener((v,f) -> {
            v.animate().cancel(); v.animate().alpha(f ? 1f : .85f).scaleX(f ? 1.15f : 1f).scaleY(f ? 1.15f : 1f)
                .setDuration(android.animation.ValueAnimator.areAnimatorsEnabled() ? 180 : 0).start();
        });
        control.setOnClickListener(v -> action.run()); return control;
    }

    private void openStockHome() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> candidates = new ArrayList<>();
        for(ResolveInfo match : getPackageManager().queryIntentActivities(home,PackageManager.MATCH_DEFAULT_ONLY)) {
            android.content.pm.ActivityInfo info=match.activityInfo;
            if(info == null || !info.exported || !info.enabled || !info.applicationInfo.enabled) continue;
            if(info.permission != null && checkSelfPermission(info.permission) != PackageManager.PERMISSION_GRANTED) continue;
            if(!StockHomePolicy.isCandidate(getPackageName(),info.packageName,info.name)) continue;
            candidates.add(match);
        }
        String saved=prefs.getString("stock_home","");
        ResolveInfo preferred=getPackageManager().resolveActivity(home,PackageManager.MATCH_DEFAULT_ONLY);
        for(ResolveInfo match:candidates) {
            ComponentName component=new ComponentName(match.activityInfo.packageName,match.activityInfo.name);
            if(component.flattenToString().equals(saved)) { launchStockHome(component); return; }
        }
        if(preferred != null && preferred.activityInfo != null) {
            for(ResolveInfo match:candidates) if(match.activityInfo.packageName.equals(preferred.activityInfo.packageName)
                    && match.activityInfo.name.equals(preferred.activityInfo.name)) {
                launchStockHome(new ComponentName(match.activityInfo.packageName,match.activityInfo.name)); return;
            }
        }
        if(candidates.size()==1) {
            ResolveInfo match=candidates.get(0);
            launchStockHome(new ComponentName(match.activityInfo.packageName,match.activityInfo.name)); return;
        }
        LinearLayout content=panel(); content.addView(text("Original TV home",24,Color.WHITE));
        Dialog chooser=dialog(content,530);
        if(candidates.isEmpty()) {
            content.addView(paragraph("No other enabled home screen is available. Enable the original launcher in TV Settings, then try again."));
            addAction(content,"Open TV Settings",() -> { if(!startSafely(new Intent(Settings.ACTION_SETTINGS))) toast("TV Settings unavailable"); });
        } else {
            content.addView(paragraph("Choose your original home screen. The dot will open it directly next time."));
            for(ResolveInfo match:candidates) {
                ComponentName component=new ComponentName(match.activityInfo.packageName,match.activityInfo.name);
                addAction(content,match.loadLabel(getPackageManager()).toString(),() -> { chooser.dismiss(); launchStockHome(component); });
            }
        }
        addAction(content,"Back",chooser::dismiss); chooser.show();
    }

    private void launchStockHome(ComponentName component) {
        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        HomeHelperState.suspend(this);
        if(startSafely(intent)) {
            prefs.edit().putString("stock_home",component.flattenToString()).apply();
            toast("Original home. Open Mountain TV from Apps to return.");
        } else {
            HomeHelperState.resume(this); prefs.edit().remove("stock_home").apply();
            toast("Original home couldn't open. Check that it is enabled in TV Settings.");
        }
    }

    private void launchApp(AppCatalog.App app) {
        Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(app.installedPackage);
        if (launch == null) launch = getPackageManager().getLaunchIntentForPackage(app.installedPackage);
        if (launch == null || !startSafely(launch)) toast("Couldn't open " + app.name + ". Try its app from TV Settings.");
    }

    private void showMissing(AppCatalog.App app) {
        LinearLayout content = panel();
        content.addView(text(app.name + " isn't installed",24,Color.WHITE));
        content.addView(paragraph("Install the TV app from Google Play, then return here to open it."));
        Dialog dialog = dialog(content, 530);
        addAction(content, "Open Google Play", () -> {
            Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app.packages[0]));
            if (!startSafely(market)) toast("Open Google Play from the TV's app list.");
            dialog.dismiss();
        });
        addAction(content, "Close", dialog::dismiss);
        dialog.show();
    }

    private void openGoogleSearch() { openGoogleSearch(false); }
    private void openGoogleSearch(boolean voice) {
        handler.removeCallbacks(searchReturned);
        HomeHelperState.beginSearch();
        if(!GoogleSearchLauncher.open(this,voice)) {
            HomeHelperState.endSearch();
            toast("Google search is unavailable. Check that the Google app is enabled in TV Settings.");
        }
    }

    private void showLauncherSettings() {
        LinearLayout content=panel();
        content.addView(text("Launcher settings",25,Color.WHITE));
        content.addView(paragraph("Search opens your TV’s existing Google search. Google handles voice input, typed queries and streaming results."));
        Dialog settings=dialog(content,570);
        addAction(content,"Home helper settings",() -> {
            if(!startSafely(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))) toast("Open Accessibility in TV Settings.");
        });
        addAction(content,"Close",settings::dismiss); settings.show();
    }

    private boolean startSafely(Intent intent) {
        try {
            if (getPackageManager().resolveActivity(intent,0) == null) return false;
            startActivity(intent); return true;
        } catch (android.content.ActivityNotFoundException | SecurityException e) { return false; }
    }

    private TextView text(String value,float size,int color) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size);
        text.setTextColor(color); text.setTypeface(font); text.setIncludeFontPadding(false); text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }
    private TextView paragraph(String value) {
        TextView paragraph = text(value,12,0xCCFFFFFF); paragraph.setPadding(0,dp(12),0,dp(12));
        paragraph.setLineSpacing(dp(3),1f); return paragraph;
    }
    private LinearLayout row() { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); return row; }
    private LinearLayout panel() {
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(26),dp(24),dp(26),dp(20));
        content.setBackground(new FrostedGlassDrawable(content,scene,glassBitmap,0xD414212D,dp(22)));
        return content;
    }
    private Dialog dialog(LinearLayout content,int width) {
        Dialog dialog = new Dialog(this) {
            private boolean counted;
            @Override protected void onStart() {
                super.onStart(); if (!counted) { counted = true; openPanels++; animateBackdrop(); }
            }
            @Override protected void onStop() {
                if (counted) { counted = false; openPanels = Math.max(0,openPanels-1); animateBackdrop(); }
                super.onStop();
            }
        };
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this); scroll.setFillViewport(false); scroll.addView(content);
        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = Math.min(dp(width),getResources().getDisplayMetrics().widthPixels-dp(64));
            params.height = -2; params.dimAmount = .36f; params.gravity = Gravity.CENTER; window.setAttributes(params);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }
    private TextView button(String value,Runnable action) {
        TextView button = text(value,12,Color.WHITE); button.setGravity(Gravity.CENTER); button.setPadding(dp(14),0,dp(14),0);
        button.setId(View.generateViewId()); button.setFocusable(true); button.setClickable(true);
        button.setBackground(shape(0x20FFFFFF,Color.TRANSPARENT,22));
        button.setOnFocusChangeListener((v,f) -> animateFocus(v,f,1.025f,22));
        button.setOnClickListener(v -> action.run()); return button;
    }
    private void addAction(LinearLayout parent,String label,Runnable action) {
        TextView button = button(label,action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,dp(39)); params.topMargin = dp(7);
        parent.addView(button,params);
    }
    private Drawable shape(int fill,int border,int radius) {
        return new SmoothSurfaceDrawable(fill,dp(radius));
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_LONG).show(); }

    @Override protected void onResume() { super.onResume();
        HomeHelperService.cancelAccessories(); HomeHelperState.resume(this); active = true; refreshApps(); handler.removeCallbacks(clockTick); clockTick.run(); }
    @Override protected void onPause() { active = false; handler.removeCallbacks(clockTick); super.onPause(); }
    @Override protected void onDestroy() { backdropGeneration++; effects.shutdownNow(); handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        handler.removeCallbacks(searchReturned);
        if(focused && active && HomeHelperState.searching()) handler.postDelayed(searchReturned,700);
    }
    @Override public void onBackPressed() { if (firstTile != null) firstTile.requestFocus(); }
    @Override public boolean onKeyDown(int code,KeyEvent event) {
        if (code == KeyEvent.KEYCODE_SEARCH) { openGoogleSearch(); return true; }
        if (code == KeyEvent.KEYCODE_MENU) { showLauncherSettings(); return true; }
        return super.onKeyDown(code,event);
    }

    private final class Symbol extends Drawable {
        private final String kind;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Symbol(String kind) { this.kind = kind; setBounds(0,0,dp(20),dp(20)); }
        @Override public int getIntrinsicWidth() { return dp(20); }
        @Override public int getIntrinsicHeight() { return dp(20); }
        @Override public void draw(Canvas canvas) {
            canvas.save(); canvas.translate(getBounds().left,getBounds().top); canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
            paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.6f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            if (kind.equals("search")) { canvas.drawCircle(10,10,6,paint); canvas.drawLine(14.6f,14.6f,21,21,paint); }
            else if (kind.equals("dot")) { paint.setStyle(Paint.Style.FILL); canvas.drawCircle(12,12,1.6f,paint); }
            else if (kind.equals("bluetooth")) {
                paint.setStrokeWidth(2f);
                Path path=new Path(); path.moveTo(7.2f,7.2f); path.lineTo(16.6f,16.6f);
                path.lineTo(12,21.2f); path.lineTo(12,2.8f); path.lineTo(16.6f,7.4f);
                path.lineTo(7.2f,16.8f); canvas.drawPath(path,paint);
            }
            else if (kind.equals("wifi")) {
                paint.setStrokeWidth(2.2f); paint.setStrokeCap(Paint.Cap.BUTT);
                for(float radius:new float[]{12.6f,8.5f,4.4f}) {
                    canvas.drawArc(new android.graphics.RectF(12-radius,18.3f-radius,12+radius,18.3f+radius),220,100,false,paint);
                }
                paint.setStyle(Paint.Style.FILL); canvas.drawCircle(12,18.3f,1.45f,paint);
            }
            else { Path path = new Path(); path.moveTo(1,20); path.lineTo(9,5); path.lineTo(14,13); path.lineTo(17,9); path.lineTo(23,20); path.close(); canvas.drawPath(path,paint); }
            canvas.restore();
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
