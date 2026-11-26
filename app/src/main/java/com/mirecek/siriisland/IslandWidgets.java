package com.mirecek.siriisland;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Home-screen widgets: Theme (2×2), Floating orb (2×2) and Switch (2×1). Widgets cannot run
 * animation code or blur, so the art is drawn here as still pictures in the current theme and wave
 * style, and the layouts animate them with ViewFlippers:
 * <ul>
 *   <li>Idle: {@link #FRAMES} frames (wave drifting and breathing, glow pulsing, the orb's nebula
 *   circling) cross-fade in a loop.</li>
 *   <li>Theme change: every animated part has two slots. The new theme is drawn into the hidden
 *   slot and the flipper switches to it, so the old theme pops out and the new one pops in.</li>
 * </ul>
 * Drawing is slow (software blur), so it runs on a background thread.
 */
final class IslandWidgets {
    private IslandWidgets() {}

    /** Idle frames per loop (see widget_idle_in/out: 800 ms each, a 4.8 s loop). */
    static final int FRAMES = 6;

    /** Glow colours when a theme is spectral (Rainbow): the prism's anchors. */
    private static final int[] SPECTRAL_GLOW = {0xFF5EA8, 0x6AD5FF, 0xFF9F43, 0x7B7BFF};

    /** Slot (0/1) showing the current theme, and the theme drawn in it. */
    private static final String KEY_SLOT = "widget_slot";
    private static final String KEY_SLOT_THEME = "widget_slot_theme";

    private static final java.util.concurrent.ExecutorService RENDER =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> new Thread(r, "IslandWidgets"));

    private static final int[][] ORB = {
            {R.id.widget_orb_0_0, R.id.widget_orb_0_1, R.id.widget_orb_0_2, R.id.widget_orb_0_3, R.id.widget_orb_0_4, R.id.widget_orb_0_5},
            {R.id.widget_orb_1_0, R.id.widget_orb_1_1, R.id.widget_orb_1_2, R.id.widget_orb_1_3, R.id.widget_orb_1_4, R.id.widget_orb_1_5}};
    private static final int[][] ISLAND = {
            {R.id.widget_island_0_0, R.id.widget_island_0_1, R.id.widget_island_0_2, R.id.widget_island_0_3, R.id.widget_island_0_4, R.id.widget_island_0_5},
            {R.id.widget_island_1_0, R.id.widget_island_1_1, R.id.widget_island_1_2, R.id.widget_island_1_3, R.id.widget_island_1_4, R.id.widget_island_1_5}};
    private static final int[][] SWITCH_ICON = {
            {R.id.widget_switch_icon_0_0, R.id.widget_switch_icon_0_1, R.id.widget_switch_icon_0_2, R.id.widget_switch_icon_0_3, R.id.widget_switch_icon_0_4, R.id.widget_switch_icon_0_5},
            {R.id.widget_switch_icon_1_0, R.id.widget_switch_icon_1_1, R.id.widget_switch_icon_1_2, R.id.widget_switch_icon_1_3, R.id.widget_switch_icon_1_4, R.id.widget_switch_icon_1_5}};
    private static final int[] GLOW = {R.id.widget_glow_0, R.id.widget_glow_1};
    private static final int[] SWATCH = {R.id.widget_swatch_0, R.id.widget_swatch_1};
    private static final int[] NAME = {R.id.widget_name_0, R.id.widget_name_1};
    private static final int[] COUNT = {R.id.widget_count_0, R.id.widget_count_1};
    private static final int[] SWITCH_GLOW = {R.id.widget_switch_glow_0, R.id.widget_switch_glow_1};

    /** Prefs that change what a widget shows (null: all prefs were cleared). */
    static boolean affects(String key) {
        return key == null || Prefs.THEME.equals(key) || Prefs.WAVE_STYLE.equals(key) || Prefs.ENABLED.equals(key)
                || Prefs.WIDGET_IDLE.equals(key);
    }

    /** Latest redraw request; queued requests that are no longer the latest are skipped. */
    private static final java.util.concurrent.atomic.AtomicInteger REQUEST = new java.util.concurrent.atomic.AtomicInteger();

    /** Redraws every widget in the background; {@code done} (may be null) runs afterwards. */
    static void updateAll(Context c, Runnable done) {
        Context app = c.getApplicationContext();
        int request = REQUEST.incrementAndGet();
        RENDER.execute(() -> {
            try {
                if (request == REQUEST.get()) render(app);
            } finally {
                if (done != null) done.run();
            }
        });
    }

    private static void render(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] theme = m.getAppWidgetIds(new ComponentName(c, ThemeWidget.class));
        int[] orb = m.getAppWidgetIds(new ComponentName(c, OrbWidget.class));
        int[] sw = m.getAppWidgetIds(new ComponentName(c, SwitchWidget.class));
        if (theme.length + orb.length + sw.length == 0) return;
        int slot = slotFor(c, Prefs.theme(c));
        if (theme.length > 0) m.updateAppWidget(theme, themeViews(c, slot));
        if (orb.length > 0) m.updateAppWidget(orb, orbViews(c, slot));
        if (sw.length > 0) m.updateAppWidget(sw, switchViews(c, slot));
    }

    /**
     * The slot to draw theme {@code t} into: the shown one when the theme is unchanged (or the
     * theme-change animation is off, so the new theme simply replaces the old), the hidden one
     * when it changed (the flipper then animates over to it).
     */
    private static int slotFor(Context c, Themes.Theme t) {
        android.content.SharedPreferences p = Prefs.get(c);
        int slot = p.getInt(KEY_SLOT, 0);
        String drawn = p.getString(KEY_SLOT_THEME, null);
        if (t.id.equals(drawn)) return slot;
        if (drawn != null && Prefs.widgetThemeAnim(c)) slot = 1 - slot;
        p.edit().putInt(KEY_SLOT, slot).putString(KEY_SLOT_THEME, t.id).apply();
        return slot;
    }

    /** Whether the idle loop plays: the user's toggle, and not with Android's "Remove animations". */
    private static boolean idle(Context c) {
        return Prefs.widgetIdle(c) && !Prefs.reduceMotion(c);
    }

    /** One loop of idle frames; all the same picture when the idle loop is off. */
    private static Bitmap[] frames(Context c, java.util.function.Function<Float, Bitmap> draw) {
        Bitmap[] out = new Bitmap[FRAMES];
        boolean still = !idle(c);
        for (int i = 0; i < FRAMES; i++) {
            // The same Bitmap object is sent once (RemoteViews caches bitmaps by identity).
            out[i] = still && i > 0 ? out[0] : draw.apply((float) (2 * Math.PI * i / FRAMES));
        }
        return out;
    }

    /**
     * Idle flippers run every 800 ms, or practically never when the loop is off (they cannot be
     * stopped from a widget update). A new interval takes effect the next time the home screen is
     * shown, which is always the case after changing the setting in the app.
     */
    private static void idleFlippers(RemoteViews v, Context c, int idle0, int idle1) {
        int interval = idle(c) ? 800 : Integer.MAX_VALUE;
        v.setInt(idle0, "setFlipInterval", interval);
        v.setInt(idle1, "setFlipInterval", interval);
    }

    // --- Views -------------------------------------------------------------------------------

    private static RemoteViews themeViews(Context c, int slot) {
        Themes.Theme t = Prefs.theme(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_theme);
        v.setImageViewBitmap(GLOW[slot], cardGlowArt(c, t, 120));
        Bitmap[] f = frames(c, phase -> islandArt(c, t, 156, 50, true, phase));
        for (int i = 0; i < FRAMES; i++) v.setImageViewBitmap(ISLAND[slot][i], f[i]);
        v.setImageViewBitmap(SWATCH[slot], swatchArt(c, t));
        v.setTextViewText(NAME[slot], t.name);
        v.setTextViewText(COUNT[slot], (indexOf(t) + 1) + " / " + Themes.ALL.length);
        for (int id : new int[]{R.id.widget_glow_slots, R.id.widget_preview_slots, R.id.widget_swatch_slots, R.id.widget_label_slots}) {
            v.setDisplayedChild(id, slot);
        }
        idleFlippers(v, c, R.id.widget_island_idle0, R.id.widget_island_idle1);
        v.setOnClickPendingIntent(R.id.widget_prev, broadcast(c, ThemeWidget.class, ThemeWidget.ACTION_PREV));
        v.setOnClickPendingIntent(R.id.widget_next, broadcast(c, ThemeWidget.class, ThemeWidget.ACTION_NEXT));
        v.setOnClickPendingIntent(R.id.widget_preview_slots, activity(c, new Intent(c, MainActivity.class)));
        v.setContentDescription(R.id.widget_preview_slots, "Theme " + t.name + ". Open Voice Island");
        return v;
    }

    private static RemoteViews orbViews(Context c, int slot) {
        Themes.Theme t = Prefs.theme(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_orb);
        Bitmap[] f = frames(c, phase -> orbArt(c, t, 150, phase));
        for (int i = 0; i < FRAMES; i++) v.setImageViewBitmap(ORB[slot][i], f[i]);
        v.setDisplayedChild(R.id.widget_orb_slots, slot);
        idleFlippers(v, c, R.id.widget_orb_idle0, R.id.widget_orb_idle1);
        v.setOnClickPendingIntent(android.R.id.background, activity(c, new Intent(c, AskActivity.class)));
        return v;
    }

    private static RemoteViews switchViews(Context c, int slot) {
        boolean on = Prefs.enabled(c);
        Themes.Theme t = Prefs.theme(c);
        Bitmap glow = pillGlowArt(c, t, on);
        Bitmap[] icon = frames(c, phase -> islandArt(c, t, 46, 22, false, phase));
        Bitmap toggle = toggleArt(c, on);
        // Narrow (the default 2×1): island icon and toggle; wider: the name and state too.
        java.util.Map<android.util.SizeF, RemoteViews> sizes = new java.util.HashMap<>();
        sizes.put(new android.util.SizeF(100, 40), switchLayout(c, slot, on, t, glow, icon, toggle, false));
        sizes.put(new android.util.SizeF(210, 40), switchLayout(c, slot, on, t, glow, icon, toggle, true));
        return new RemoteViews(sizes);
    }

    private static RemoteViews switchLayout(Context c, int slot, boolean on, Themes.Theme t,
            Bitmap glow, Bitmap[] icon, Bitmap toggle, boolean withText) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_switch);
        v.setImageViewBitmap(SWITCH_GLOW[slot], glow);
        for (int i = 0; i < FRAMES; i++) {
            v.setImageViewBitmap(SWITCH_ICON[slot][i], icon[i]);
            v.setInt(SWITCH_ICON[slot][i], "setImageAlpha", on ? 255 : 90);
        }
        v.setDisplayedChild(R.id.widget_switch_glow_slots, slot);
        v.setDisplayedChild(R.id.widget_switch_icon_slots, slot);
        idleFlippers(v, c, R.id.widget_switch_idle0, R.id.widget_switch_idle1);
        v.setViewVisibility(R.id.widget_switch_texts, withText ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.widget_switch_space, withText ? View.GONE : View.VISIBLE);
        v.setTextViewText(R.id.widget_switch_state, on ? "On · " + t.name : "Off");
        v.setImageViewBitmap(R.id.widget_switch_toggle, toggle);
        v.setOnClickPendingIntent(android.R.id.background, broadcast(c, SwitchWidget.class, SwitchWidget.ACTION_TOGGLE));
        v.setContentDescription(android.R.id.background, "Voice Island " + (on ? "on" : "off") + ". Double-tap to switch");
        return v;
    }

    static int indexOf(Themes.Theme t) {
        for (int i = 0; i < Themes.ALL.length; i++) if (Themes.ALL[i] == t) return i;
        return 0;
    }

    private static PendingIntent broadcast(Context c, Class<?> receiver, String action) {
        Intent i = new Intent(c, receiver).setAction(action);
        return PendingIntent.getBroadcast(c, action.hashCode(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent activity(Context c, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, i.getComponent().getClassName().hashCode(), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // --- Art helpers ---------------------------------------------------------------------------

    /** The theme's four glow colours, opaque. */
    private static int[] glowColors(Themes.Theme t) {
        int[] src = t.spectral ? SPECTRAL_GLOW : t.colors;
        int[] out = new int[4];
        for (int i = 0; i < 4; i++) out[i] = 0xFF000000 | src[i];
        return out;
    }

    private static int alpha(int color, float a) {
        return Math.round(255 * a) << 24 | (color & 0xFFFFFF);
    }

    private static float sin(float a) { return (float) Math.sin(a); }

    private static float cos(float a) { return (float) Math.cos(a); }

    /**
     * {@code box} for idle frame {@code phase}: shifted sideways by up to {@code drift} and stretched
     * vertically by up to 18 % (out of step with the drift, so the wave seems to flow and breathe).
     */
    private static RectF idleBox(RectF box, float phase, float drift) {
        float dx = drift * sin(phase);
        float grow = box.height() * 0.09f * sin(phase + 1.2f);
        return new RectF(box.left + dx, box.top - grow, box.right + dx, box.bottom + grow);
    }

    /** A soft blurred blob of colour. */
    private static void blob(Canvas canvas, float cx, float cy, float rx, float ry, int color, float blur) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
        canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, p);
    }

    /**
     * The theme's wave (as on the Look page's cards) drawn into {@code box}, clipped to {@code clip}.
     * It is screened over what is already there, so the wave's black tile adds nothing, and fades
     * out towards the left and right ends so it melts into the glass instead of being cut.
     */
    private static void drawWave(Context c, Canvas canvas, Themes.Theme t, RectF box, Path clip) {
        MiniWaveView wave = new MiniWaveView(c, t, MiniWaveView.modeFor(Prefs.waveStyle(c)));
        int w = Math.round(box.width()), h = Math.round(box.height());
        wave.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        wave.layout(0, 0, w, h);
        int save = canvas.save();
        canvas.clipPath(clip);
        Paint screen = new Paint();
        screen.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        int layer = canvas.saveLayer(box, screen);
        canvas.translate(box.left, box.top);
        wave.draw(canvas);
        canvas.translate(-box.left, -box.top);
        Paint fade = new Paint();
        fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        fade.setShader(new LinearGradient(box.left, 0, box.right, 0,
                new int[]{0x00000000, 0xFF000000, 0xFF000000, 0x00000000},
                new float[]{0.04f, 0.3f, 0.7f, 0.96f}, Shader.TileMode.CLAMP));
        canvas.drawRect(box, fade);
        canvas.restoreToCount(layer);
        canvas.restoreToCount(save);
    }

    /** Glass rim: white at the top left, faint on the sides, the theme's colour at the bottom right. */
    private static Paint rim(RectF r, float width, float alpha, int tint) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                new int[]{alpha(0xFFFFFF, alpha), alpha(0xFFFFFF, alpha * 0.12f), alpha(tint, alpha * 0.8f)},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        return p;
    }

    /** A crisp curved highlight along the top inside edge of a glass shape. */
    private static void topSheen(Canvas canvas, RectF r, float radius, Path clip, float strength) {
        int save = canvas.save();
        canvas.clipPath(clip);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float h = r.height() * 0.42f;
        p.setShader(new LinearGradient(0, r.top, 0, r.top + h,
                alpha(0xFFFFFF, strength), 0x00FFFFFF, Shader.TileMode.CLAMP));
        float inset = r.width() * 0.08f;
        canvas.drawRoundRect(r.left + inset, r.top + r.height() * 0.035f, r.right - inset, r.top + h,
                radius, radius, p);
        canvas.restoreToCount(save);
    }

    // --- Art ---------------------------------------------------------------------------------

    /**
     * The island capsule: black glass with the theme's wave melting in from the sides, a coloured
     * bloom underneath (when {@code bloom}), a top sheen and a lit rim. {@code phase} (0..2π) moves
     * the idle loop: the wave drifts sideways and breathes, the bloom pulses.
     */
    static Bitmap islandArt(Context c, Themes.Theme t, int wDp, int hDp, boolean bloom, float phase) {
        float dp = c.getResources().getDisplayMetrics().density;
        float pad = bloom ? 16 * dp : 1.5f * dp;
        int bw = Math.round(wDp * dp + 2 * pad), bh = Math.round(hDp * dp + 2 * pad);
        Bitmap b = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        int[] g = glowColors(t);
        RectF r = new RectF(pad, pad, bw - pad, bh - pad);
        float radius = r.height() / 2f;

        if (bloom) {
            float cy = r.bottom - r.height() * 0.1f;
            float a = 0.55f * (0.8f + 0.2f * (float) Math.sin(phase + 2f));
            float sway = r.width() * 0.04f * (float) Math.sin(phase);
            blob(canvas, r.centerX() - r.width() * 0.18f + sway, cy, r.width() * 0.3f, r.height() * 0.32f, alpha(g[0], a), 12 * dp);
            blob(canvas, r.centerX() + r.width() * 0.18f + sway, cy, r.width() * 0.3f, r.height() * 0.32f, alpha(g[2], a), 12 * dp);
        }

        Path clip = new Path();
        clip.addRoundRect(r, radius, radius, Path.Direction.CW);
        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(0xFF000000);
        canvas.drawPath(clip, body);
        drawWave(c, canvas, t, idleBox(new RectF(r.left, r.top - r.height() * 0.15f, r.right, r.bottom + r.height() * 0.15f),
                phase, r.width() * 0.08f), clip);
        topSheen(canvas, r, radius, clip, 0.1f);
        canvas.drawRoundRect(r, radius, radius, rim(r, Math.max(1f, 1.2f * dp), 0.8f, g[2]));
        return b;
    }

    /**
     * A Siri-style glass orb floating on the wallpaper: a coloured halo behind it, a dark body with
     * a nebula of the theme's colours inside, the wave across the middle, glass depth towards the
     * edge, a crisp top highlight, a coloured reflection at the bottom and a lit rim. {@code phase}
     * (0..2π) moves the idle loop: the nebula circles, the wave drifts and breathes, the halo pulses.
     */
    static Bitmap orbArt(Context c, Themes.Theme t, int sizeDp, float phase) {
        float dp = c.getResources().getDisplayMetrics().density;
        int size = Math.round(sizeDp * dp);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        int[] g = glowColors(t);
        float cx = size / 2f, cy = size / 2f, r = size * 0.31f;

        // Halo: the theme's light spilling onto the wallpaper, warmer below, cooler above. Sized to
        // fade out before the bitmap's edge, so it never shows a square cut.
        float pulse = 0.85f + 0.15f * (float) Math.sin(phase);
        blob(canvas, cx - r * 0.2f, cy + r * 0.3f, r * 0.85f, r * 0.75f, alpha(g[0], 0.4f * pulse), r * 0.3f);
        blob(canvas, cx + r * 0.25f, cy - r * 0.2f, r * 0.8f, r * 0.75f, alpha(g[3], 0.36f * pulse), r * 0.3f);
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(0x73000000);
        shadow.setMaskFilter(new BlurMaskFilter(r * 0.12f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(cx, cy + r * 0.06f, r, shadow);

        Path clip = new Path();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clip);
        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setShader(new RadialGradient(cx, cy - r * 0.2f, r * 1.2f, 0xFF15151C, 0xFF000000, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, body);
        // Nebula: the theme's colours drifting round inside the glass.
        float d = r * 0.2f;
        blob(canvas, cx - r * 0.45f + d * cos(phase), cy + r * 0.55f + d * sin(phase), r * 0.7f, r * 0.45f, alpha(g[0], 0.55f), r * 0.35f);
        blob(canvas, cx + r * 0.5f + d * cos(phase + 2.1f), cy + r * 0.45f + d * sin(phase + 2.1f), r * 0.65f, r * 0.45f, alpha(g[2], 0.55f), r * 0.35f);
        blob(canvas, cx + r * 0.1f + d * cos(phase + 4.2f), cy - r * 0.6f + d * sin(phase + 4.2f), r * 0.6f, r * 0.35f, alpha(g[3], 0.35f), r * 0.35f);
        canvas.restoreToCount(save);

        drawWave(c, canvas, t, idleBox(new RectF(cx - r * 1.1f, cy - r * 0.62f, cx + r * 1.1f, cy + r * 0.62f), phase, r * 0.12f), clip);

        save = canvas.save();
        canvas.clipPath(clip);
        // Glass depth: clear in the middle, darker towards the edge.
        Paint depth = new Paint(Paint.ANTI_ALIAS_FLAG);
        depth.setShader(new RadialGradient(cx, cy, r, new int[]{0x00000000, 0x00000000, 0xB3000000},
                new float[]{0f, 0.68f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, depth);
        // Coloured reflection hugging the bottom edge.
        Paint low = new Paint(Paint.ANTI_ALIAS_FLAG);
        low.setStyle(Paint.Style.STROKE);
        low.setStrokeWidth(r * 0.09f);
        low.setMaskFilter(new BlurMaskFilter(r * 0.06f, BlurMaskFilter.Blur.NORMAL));
        low.setShader(new LinearGradient(cx - r, 0, cx + r, 0, new int[]{alpha(g[0], 0f), alpha(g[1], 0.9f), alpha(g[2], 0.9f), alpha(g[3], 0f)},
                null, Shader.TileMode.CLAMP));
        RectF inner = new RectF(cx - r * 0.93f, cy - r * 0.93f, cx + r * 0.93f, cy + r * 0.93f);
        canvas.drawArc(inner, 35, 110, false, low);
        // Top highlight: a crisp crescent of light, brightest at the top.
        Paint spec = new Paint(Paint.ANTI_ALIAS_FLAG);
        spec.setShader(new LinearGradient(0, cy - r, 0, cy - r * 0.45f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawOval(cx - r * 0.62f, cy - r * 0.93f, cx + r * 0.62f, cy - r * 0.3f, spec);
        canvas.restoreToCount(save);

        RectF bounds = new RectF(cx - r, cy - r, cx + r, cy + r);
        canvas.drawCircle(cx, cy, r, rim(bounds, Math.max(1f, 1.4f * dp), 0.9f, g[2]));
        return b;
    }

    /**
     * The theme card's backdrop: dark glass lit from behind by the theme's colours, strongest
     * under the island, with a sheen at the top. Stretched to the widget's size (it is all soft).
     */
    static Bitmap cardGlowArt(Context c, Themes.Theme t, int sizeDp) {
        float dp = c.getResources().getDisplayMetrics().density;
        int s = Math.round(sizeDp * dp);
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        int[] g = glowColors(t);
        Paint base = new Paint();
        base.setShader(new LinearGradient(0, 0, 0, s, 0xE62A2A30, 0xE6111114, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, s, s, base);
        float cy = s * 0.38f;
        blob(canvas, s * 0.3f, cy, s * 0.32f, s * 0.2f, alpha(g[0], 0.32f), s * 0.16f);
        blob(canvas, s * 0.7f, cy, s * 0.32f, s * 0.2f, alpha(g[2], 0.32f), s * 0.16f);
        blob(canvas, s * 0.5f, s * 0.95f, s * 0.45f, s * 0.15f, alpha(g[3], 0.18f), s * 0.16f);
        Paint sheen = new Paint();
        sheen.setShader(new LinearGradient(0, 0, 0, s * 0.25f, 0x1FFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, s, s * 0.25f, sheen);
        return b;
    }

    /** The switch capsule's backdrop: dark glass with a faint glow of the theme while it is on. */
    static Bitmap pillGlowArt(Context c, Themes.Theme t, boolean on) {
        float dp = c.getResources().getDisplayMetrics().density;
        int w = Math.round(180 * dp), h = Math.round(64 * dp);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        Paint base = new Paint();
        base.setShader(new LinearGradient(0, 0, 0, h, 0xE62A2A30, 0xE6111114, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, base);
        if (on) {
            int[] g = glowColors(t);
            blob(canvas, w * 0.14f, h * 0.55f, w * 0.18f, h * 0.45f, alpha(g[0], 0.3f), h * 0.4f);
            blob(canvas, w * 0.3f, h * 0.55f, w * 0.18f, h * 0.45f, alpha(g[2], 0.24f), h * 0.4f);
        }
        Paint sheen = new Paint();
        sheen.setShader(new LinearGradient(0, 0, 0, h * 0.45f, 0x1FFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h * 0.45f, sheen);
        return b;
    }

    /** The theme's four colours as small glass dots. */
    static Bitmap swatchArt(Context c, Themes.Theme t) {
        float dp = c.getResources().getDisplayMetrics().density;
        int[] g = glowColors(t);
        float d = 8 * dp, gap = 5 * dp;
        int w = Math.round(4 * d + 3 * gap + 2 * dp), h = Math.round(d + 2 * dp);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Math.max(1f, 0.8f * dp));
        ring.setColor(0x4DFFFFFF);
        for (int i = 0; i < 4; i++) {
            float x = dp + d / 2f + i * (d + gap), y = h / 2f;
            p.setColor(g[i]);
            canvas.drawCircle(x, y, d / 2f, p);
            canvas.drawCircle(x, y, d / 2f, ring);
        }
        return b;
    }

    /** An iOS switch picture: green or grey track with a top sheen, white knob with a soft shadow. */
    static Bitmap toggleArt(Context c, boolean on) {
        float dp = c.getResources().getDisplayMetrics().density;
        float pad = 3 * dp;
        int w = Math.round(51 * dp + 2 * pad), h = Math.round(31 * dp + 2 * pad);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        RectF r = new RectF(pad, pad, w - pad, h - pad);
        float rr = r.height() / 2f;
        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setColor(on ? Ui.SUCCESS : 0x5C787880);
        canvas.drawRoundRect(r, rr, rr, track);
        float kr = rr - 2 * dp, kx = on ? r.right - rr : r.left + rr;
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(0x40000000);
        shadow.setMaskFilter(new BlurMaskFilter(2.5f * dp, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(kx, r.centerY() + 1.2f * dp, kr, shadow);
        Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
        knob.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0xFFFFFFFF, 0xFFF2F2F7, Shader.TileMode.CLAMP));
        canvas.drawCircle(kx, r.centerY(), kr, knob);
        return b;
    }
}
