package com.mirecek.siriisland;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Switch;

/**
 * iOS 27 switch after Kyant's LiquidToggle (AndroidLiquidGlass catalog, Apache 2.0), the toggle
 * ClearPDF, Convx and BitChord use: a 64x28 dp capsule track that fades from grey to green and a
 * 40x24 dp white thumb. While held the thumb grows to 1.5x and turns into a clear glass lens that
 * shows the track through it (scaled into the lens), with a lit rim and an inner shadow; it squashes
 * along its own speed. Tap toggles, drag slides it across, a cancelled drag goes back.
 */
final class LiquidToggle extends View {
    interface OnChange {
        void changed(boolean on);
    }

    private static final float TRACK_W = 64, TRACK_H = 28, THUMB_W = 40, THUMB_H = 24, PAD = 2;
    private static final float DRAG_W = TRACK_W - THUMB_W - 2 * PAD;
    /** Room around the track for the 1.5x lens. */
    private static final float MARGIN = 12;
    private static final float PRESSED_SCALE = 1.5f;
    private static final int TRACK_OFF = 0x5C787880;
    private static final int TRACK_ON = 0xFF30D158;

    private final float dp;
    private final Spring value = new Spring();
    private final Spring press = new Spring();
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint surface = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF thumb = new RectF();
    private final Path clip = new Path();

    private boolean on;
    private OnChange listener;
    private long lastFrame;
    private boolean reduceMotion;
    private boolean held, didDrag, onAtPress;
    private float lastX, downX;

    LiquidToggle(Context c, boolean checked) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        on = checked;
        value.snap(on ? 1f : 0f);
        surface.setColor(0xFFFFFFFF);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.2f * dp);
        inner.setStyle(Paint.Style.STROKE);
        inner.setStrokeWidth(4f * dp);
        inner.setMaskFilter(new BlurMaskFilter(4f * dp, BlurMaskFilter.Blur.NORMAL));
        shadow.setColor(0x33000000);
        shadow.setMaskFilter(new BlurMaskFilter(4f * dp, BlurMaskFilter.Blur.NORMAL));
        setClickable(true);
        setFocusable(true);
        reduceMotion = Prefs.reduceMotion(c);
        setStateDescription(on ? "On" : "Off");
    }

    void setOnChange(OnChange l) { listener = l; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(Math.round((TRACK_W + 2 * MARGIN) * dp), Math.round(Math.max(48, TRACK_H + 2 * MARGIN) * dp));
    }

    private void commit(boolean next) {
        value.target = next ? 1f : 0f;
        if (next != on) {
            on = next;
            setStateDescription(on ? "On" : "Off");
            if (listener != null) listener.changed(on);
        }
        invalidate();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        commit(!on);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                held = true;
                didDrag = false;
                onAtPress = on;
                downX = lastX = e.getX();
                reduceMotion = Prefs.reduceMotion(getContext());
                press.target = reduceMotion ? 0f : 1f;
                lastFrame = 0;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                if (!held) return false;
                if (!didDrag && Math.abs(e.getX() - downX) > 6 * dp) {
                    didDrag = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (didDrag) {
                    value.x = Math.max(0f, Math.min(1f, value.x + (e.getX() - lastX) / (DRAG_W * dp)));
                    value.target = value.x;
                    invalidate();
                }
                lastX = e.getX();
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                if (!held) return false;
                held = false;
                press.target = 0f;
                if (didDrag) commit(value.x >= 0.5f);
                else performClick();
                return true;
            }
            case MotionEvent.ACTION_CANCEL -> {
                if (!held) return false;
                held = false;
                press.target = 0f;
                commit(onAtPress);
                return true;
            }
            default -> { return held; }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (!(held && didDrag)) {
            if (reduceMotion) value.snap(value.target);
            else value.step(dt, 20f, 0.7f);
        }
        if (reduceMotion) press.snap(press.target);
        else press.step(dt, 30f, 0.62f);
        float f = Math.max(0f, Math.min(1f, value.x));
        float p = Math.max(0f, Math.min(1f, press.x));

        float ox = (getWidth() - TRACK_W * dp) / 2f, oy = (getHeight() - TRACK_H * dp) / 2f;
        trackRect.set(ox, oy, ox + TRACK_W * dp, oy + TRACK_H * dp);
        float tr = trackRect.height() / 2f;
        track.setColor(mix(TRACK_OFF, TRACK_ON, f));
        canvas.drawRoundRect(trackRect, tr, tr, track);

        // Thumb: grows with the press, squashes along its own speed (LiquidToggle's layer block).
        float scale = 1f + (PRESSED_SCALE - 1f) * press.x;
        float v = reduceMotion ? 0f : Math.abs(value.v) / 50f;
        float sx = scale / (1f - Math.min(0.2f, v * 0.75f));
        float sy = scale * (1f - Math.min(0.2f, v * 0.25f));
        float cx = ox + (PAD + DRAG_W * f + THUMB_W / 2f) * dp, cy = trackRect.centerY();
        float hw = THUMB_W * dp * sx / 2f, hh = THUMB_H * dp * sy / 2f;
        thumb.set(cx - hw, cy - hh, cx + hw, cy + hh);
        float r = thumb.height() / 2f;

        thumb.offset(0, 1f * dp);
        canvas.drawRoundRect(thumb, r, r, shadow);
        thumb.offset(0, -1f * dp);

        if (p > 0.01f) {
            // The lens: what is behind it (the card) with the track seen through the glass, shrunk to
            // 0.75 about the lens centre, so a small track floats inside the clear thumb.
            clip.rewind();
            clip.addRoundRect(thumb, r, r, Path.Direction.CW);
            int save = canvas.save();
            canvas.clipPath(clip);
            canvas.drawColor(Ui.CARD);
            int inside = canvas.save();
            canvas.scale(lerp(2f / 3f, 0.75f, p), lerp(0f, 0.75f, p), cx, cy);
            canvas.drawRoundRect(trackRect, tr, tr, track);
            canvas.restoreToCount(inside);
            inner.setColor(Math.round(0x40 * p) << 24);
            canvas.drawRoundRect(thumb, r, r, inner);
            canvas.restoreToCount(save);
            int a = Math.round(0xE6 * p);
            rim.setShader(new LinearGradient(thumb.left, thumb.top, thumb.right, thumb.bottom,
                    new int[]{a << 24 | 0xFFFFFF, (a / 4) << 24 | 0xFFFFFF, (a / 2) << 24 | 0xFFFFFF},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(thumb, r, r, rim);
        }
        surface.setAlpha(Math.round(255 * (1f - p)));
        canvas.drawRoundRect(thumb, r, r, surface);

        boolean settled = value.settled(0.001f) && press.settled(0.002f);
        if (!settled || held) postInvalidateOnAnimation();
        else lastFrame = 0;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static int mix(int a, int b, float t) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16
                | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Switch.class.getName());
        info.setCheckable(true);
        info.setChecked(on);
    }
}
