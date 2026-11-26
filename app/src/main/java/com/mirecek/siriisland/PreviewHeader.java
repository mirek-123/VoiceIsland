package com.mirecek.siriisland;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * The live island preview shared by every settings page: a rounded card over a lock-screen-like
 * backdrop that collapses from its full height to a strip (main page scroll, sub-pages).
 */
final class PreviewHeader extends FrameLayout {
    final IslandView island;
    final VoiceActivity voice;
    private final Ui ui;
    private final ImageView wallpaper;
    private final int fullHeight;
    private final int stripHeight;
    private Bitmap wallpaperBitmap;
    private float collapse;
    private ValueAnimator anim;

    PreviewHeader(Context c, Ui ui) {
        super(c);
        this.ui = ui;
        fullHeight = ui.px(200);
        stripHeight = ui.px(104);
        setBackgroundColor(0xFF101018);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), ui.px(22));
            }
        });
        setClipToOutline(true);
        wallpaper = new ImageView(c);
        wallpaper.setScaleType(ImageView.ScaleType.MATRIX);
        wallpaper.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(wallpaper, new LayoutParams(Ui.MATCH, Ui.MATCH));
        island = new IslandView(c);
        voice = new VoiceActivity(c);
        island.setLevelSource(voice);
        addView(island, new LayoutParams(Ui.MATCH, Ui.MATCH));
        island.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) relayout();
        });
        applyPrefs();
        island.showStatic();
        setContentDescription("Live preview of the voice island");
    }

    /** Re-reads the glass and look settings into the preview island. */
    void applyPrefs() {
        Context c = getContext();
        island.setLiquid(Prefs.liquidGlass(c));
        island.applyLook(c);
        relayout();
    }

    int heightFor(float f) { return Math.round(fullHeight + (stripHeight - fullHeight) * f); }

    /** Scroll distance over which the main page collapses the card into the strip. */
    int collapseRange() { return fullHeight - stripHeight; }

    /** 0 = full card, 1 = strip. */
    void setCollapse(float f) {
        f = Math.max(0f, Math.min(1f, f));
        collapse = f;
        ViewGroup.LayoutParams lp = getLayoutParams();
        int h = heightFor(f);
        if (lp != null && lp.height != h) {
            lp.height = h;
            setLayoutParams(lp);
        }
    }

    /** Collapse driven directly by scrolling: stops any running collapse animation first so it cannot fight the scroll. */
    void scrollCollapse(float f) {
        if (anim != null) anim.cancel();
        setCollapse(f);
    }

    void animateCollapse(float to) {
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(collapse, to);
        anim.setDuration(350);
        anim.setInterpolator(new PathInterpolator(0.32f, 0.72f, 0f, 1f));
        anim.addUpdateListener(a -> setCollapse((float) a.getAnimatedValue()));
        anim.start();
    }

    private void relayout() {
        int w = island.getWidth(), h = island.getHeight();
        if (w == 0 || h == 0) return;
        Context c = getContext();
        boolean orb = Prefs.orb(c);
        float aspect = IslandView.aspect(orb);
        float iw = Math.min(w * (orb ? 0.36f : 0.5f), (h - ui.px(14)) / aspect);
        island.setGeometry(w / 2f, ui.px(22), ui.px(14), iw, iw * aspect, ui.px(6), true);
        if (wallpaperBitmap == null || wallpaperBitmap.getWidth() != w) {
            wallpaperBitmap = sampleWallpaper(w, fullHeight);
            wallpaper.setImageBitmap(wallpaperBitmap);
            island.setBackdrop(wallpaperBitmap, new Rect(0, 0, w, fullHeight));
        }
    }

    private Bitmap sampleWallpaper(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(0xFF0B1030);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float[][] blobs = {{0.2f, 0.3f, 0.55f}, {0.85f, 0.2f, 0.45f}, {0.55f, 0.95f, 0.6f}};
        int[] colors = {0xFF2F6BFF, 0xFFFF7A3D, 0xFF9B5CFF};
        for (int i = 0; i < blobs.length; i++) {
            float[] b = blobs[i];
            p.setShader(new RadialGradient(b[0] * w, b[1] * h, b[2] * w, colors[i], colors[i] & 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
        }
        p.setShader(null);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setTextSize(ui.px(16));
        c.drawText("Tuesday, January 9", w / 2f, ui.px(70), p);
        p.setTextSize(ui.px(86));
        c.drawText("9:41", w / 2f, ui.px(160), p);
        return bmp;
    }
}
