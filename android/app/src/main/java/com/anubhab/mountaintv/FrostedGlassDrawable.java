package com.anubhab.mountaintv;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * A wallpaper-backed glass surface that works before Android 12 RenderEffect.
 *
 * Prepare one blurred wallpaper on a worker with {@link #blurWallpaper(Bitmap)},
 * then share that result between surfaces. Each surface samples the same full-scene
 * CENTER_CROP wallpaper, including when its owner is in a separate dialog window.
 * Drawing only updates a shader matrix and fills a rounded rectangle; it never
 * captures the screen, blurs a frame, allocates a bitmap, or starts a redraw loop.
 *
 * The caller owns the shared blurred bitmap and must not recycle it while a surface
 * uses it. There is no static cache, view listener, or activity reference. Dropping
 * the view backgrounds and caller's bitmap reference allows normal garbage collection.
 */
public final class FrostedGlassDrawable extends Drawable {
    private static final int MAX_BLUR_EDGE = 320;
    private static final int BOX_RADIUS = 8;
    private static final int FALLBACK_COLOR = 0x405A625E;

    private final WeakReference<View> owner;
    private final WeakReference<View> scene;
    private final Bitmap blurred;
    private final BitmapShader shader;
    private final int tint;
    private final float radiusPx;
    private final boolean pill;
    private final Paint wallpaperPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix wallpaperMatrix = new Matrix();
    private final RectF surface = new RectF();
    private final android.graphics.Path surfacePath = new android.graphics.Path();
    private final int[] ownerLocation = new int[2];
    private final int[] sceneLocation = new int[2];
    private int alpha = 255;

    /**
     * @param owner the view whose background is this drawable
     * @param scene the full-screen scene containing the original CENTER_CROP wallpaper
     * @param blurred a shared result from blurWallpaper, or null for the fallback fill
     * @param tint translucent tint drawn over the sampled wallpaper, for example 0x36FFF8ED
     * @param radiusPx corner radius in physical pixels, usually converted from dp by the caller
     */
    public FrostedGlassDrawable(View owner, View scene, Bitmap blurred, int tint, float radiusPx) {
        this(owner,scene,blurred,tint,radiusPx,false);
    }

    /** A pill uses semicircular ends sized from its actual height, without corner smoothing. */
    public FrostedGlassDrawable(View owner, View scene, Bitmap blurred, int tint, float radiusPx, boolean pill) {
        if (owner == null || scene == null) throw new IllegalArgumentException("Glass requires owner and scene views");
        this.owner = new WeakReference<>(owner);
        this.scene = new WeakReference<>(scene);
        this.blurred = blurred;
        this.tint = tint;
        this.radiusPx = Math.max(0f, radiusPx);
        this.pill = pill;
        shader = blurred != null && !blurred.isRecycled()
                ? new BitmapShader(blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) : null;
        wallpaperPaint.setShader(shader);
        tintPaint.setColor(tint);
        fallbackPaint.setColor(FALLBACK_COLOR);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        surface.set(bounds);
        if(pill) {
            surfacePath.reset();
            float radius=surface.height()/2f;
            surfacePath.addRoundRect(surface,radius,radius,android.graphics.Path.Direction.CW);
        } else {
            SmoothSurfaceDrawable.continuousPath(surfacePath,surface,radiusPx);
        }
    }

    @Override public void draw(Canvas canvas) {
        if (surface.isEmpty() || alpha == 0) return;
        View ownerView = owner.get();
        View sceneView = scene.get();
        boolean usable = shader != null && blurred != null && !blurred.isRecycled()
                && ownerView != null && sceneView != null
                && sceneView.getWidth() > 0 && sceneView.getHeight() > 0;
        if (usable) {
            // Screen coordinates also align a dialog's window with the home scene.
            ownerView.getLocationOnScreen(ownerLocation);
            sceneView.getLocationOnScreen(sceneLocation);
            float scale = Math.max(sceneView.getWidth() / (float) blurred.getWidth(),
                    sceneView.getHeight() / (float) blurred.getHeight());
            float left = sceneLocation[0] - ownerLocation[0]
                    + (sceneView.getWidth() - blurred.getWidth() * scale) * 0.5f;
            float top = sceneLocation[1] - ownerLocation[1]
                    + (sceneView.getHeight() - blurred.getHeight() * scale) * 0.5f;
            wallpaperMatrix.setScale(scale, scale);
            wallpaperMatrix.postTranslate(left, top);
            shader.setLocalMatrix(wallpaperMatrix);
            // Replace the sharp wallpaper underneath. Translucent blur leaves its
            // high-frequency detail visible and looks like a tinted sheet on TV.
            // The sampled wallpaper itself supplies the glass's visual transparency.
            wallpaperPaint.setAlpha(alpha);
            canvas.drawPath(surfacePath, wallpaperPaint);
        } else {
            canvas.drawPath(surfacePath, fallbackPaint);
        }
        // A fill rather than a stroke keeps the glass edge calm on a distant TV.
        canvas.drawPath(surfacePath, tintPaint);
    }

    @Override public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
        wallpaperPaint.setAlpha(this.alpha);
        tintPaint.setAlpha((Color.alpha(tint) * this.alpha + 127) / 255);
        fallbackPaint.setAlpha((Color.alpha(FALLBACK_COLOR) * this.alpha + 127) / 255);
        invalidateSelf();
    }

    @Override public int getAlpha() {
        return alpha;
    }

    @Override public void setColorFilter(ColorFilter filter) {
        wallpaperPaint.setColorFilter(filter);
        tintPaint.setColorFilter(filter);
        fallbackPaint.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /**
     * Build an independent, immutable blurred image on a worker thread.
     *
     * The longest edge is at most 320 pixels, so a landscape wallpaper needs well
     * under 1 MB of temporary blur storage. Three separable box passes approximate
     * a soft Gaussian blur in linear time. Neither the source pixels nor its
     * ownership change, even when it already fits inside the target size.
     */
    public static Bitmap blurWallpaper(Bitmap source) {
        if (source == null || source.isRecycled()) throw new IllegalArgumentException("Wallpaper must be a live bitmap");
        float ratio = Math.min(1f, MAX_BLUR_EDGE / (float) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, Math.round(source.getWidth() * ratio));
        int height = Math.max(1, Math.round(source.getHeight() * ratio));
        Bitmap readable = source;
        Bitmap sample = null;
        try {
            // A software Canvas cannot sample a hardware-only bitmap.
            if (source.getConfig() == Bitmap.Config.HARDWARE) {
                readable = source.copy(Bitmap.Config.ARGB_8888, false);
                if (readable == null) throw new IllegalArgumentException("Wallpaper could not be read");
            }
            sample = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas scaled = new Canvas(sample);
            Paint filter = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
            scaled.drawBitmap(readable, null, new Rect(0, 0, width, height), filter);
            int[] pixels = new int[width * height];
            int[] scratch = new int[pixels.length];
            sample.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int pass = 0; pass < 3; pass++) {
                horizontal(pixels, scratch, width, height);
                vertical(scratch, pixels, width, height);
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } finally {
            if (sample != null) sample.recycle();
            if (readable != source && readable != null) readable.recycle();
        }
    }

    private static void horizontal(int[] input, int[] output, int width, int height) {
        int diameter = BOX_RADIUS * 2 + 1;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            int a = 0, r = 0, g = 0, b = 0;
            for (int offset = -BOX_RADIUS; offset <= BOX_RADIUS; offset++) {
                int color = input[row + clamp(offset, width)];
                a += color >>> 24; r += (color >>> 16) & 255; g += (color >>> 8) & 255; b += color & 255;
            }
            for (int x = 0; x < width; x++) {
                output[row + x] = pack(a / diameter, r / diameter, g / diameter, b / diameter);
                int remove = input[row + clamp(x - BOX_RADIUS, width)];
                int add = input[row + clamp(x + BOX_RADIUS + 1, width)];
                a += (add >>> 24) - (remove >>> 24);
                r += ((add >>> 16) & 255) - ((remove >>> 16) & 255);
                g += ((add >>> 8) & 255) - ((remove >>> 8) & 255);
                b += (add & 255) - (remove & 255);
            }
        }
    }

    private static void vertical(int[] input, int[] output, int width, int height) {
        int diameter = BOX_RADIUS * 2 + 1;
        for (int x = 0; x < width; x++) {
            int a = 0, r = 0, g = 0, b = 0;
            for (int offset = -BOX_RADIUS; offset <= BOX_RADIUS; offset++) {
                int color = input[clamp(offset, height) * width + x];
                a += color >>> 24; r += (color >>> 16) & 255; g += (color >>> 8) & 255; b += color & 255;
            }
            for (int y = 0; y < height; y++) {
                output[y * width + x] = pack(a / diameter, r / diameter, g / diameter, b / diameter);
                int remove = input[clamp(y - BOX_RADIUS, height) * width + x];
                int add = input[clamp(y + BOX_RADIUS + 1, height) * width + x];
                a += (add >>> 24) - (remove >>> 24);
                r += ((add >>> 16) & 255) - ((remove >>> 16) & 255);
                g += ((add >>> 8) & 255) - ((remove >>> 8) & 255);
                b += (add & 255) - (remove & 255);
            }
        }
    }

    private static int clamp(int value, int size) {
        return Math.max(0, Math.min(size - 1, value));
    }

    private static int pack(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
