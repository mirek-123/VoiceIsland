package com.mirecek.siriisland;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * Settings slider with an iOS 27 liquid-glass thumb (after the ClearPDF recording): idle it is a
 * white 40x24 dp capsule; while dragged it springs into a clear 67x33 dp real-time glass lens that
 * refracts the pixels under it (see {@link #LENS}) and stretches with the drag speed. On release it
 * wobbles back into the capsule. It is still a {@link SeekBar}, so values, listeners, keyboard and
 * TalkBack behave as before; only the drawing is replaced.
 */
final class GlassSlider extends SeekBar {
    private static final int TRACK = 0xFF3A3A3C;
    private static final int FILL = Ui.ACCENT;

    /**
     * Real-time liquid glass: a RenderEffect over a layer holding what is actually drawn under the
     * lens (the track and its fill, re-recorded every frame), so the glass refracts real pixels.
     * The lens is a capsule with a round bezel: the bezel's slope pushes samples inward along the
     * outline normal, so the rim bends the track into the edge and the middle magnifies it slightly.
     * Red and blue are sampled a little apart (dispersion); a specular glint from the top-left and a
     * weaker counter-glint finish the glass.
     */
    private static final String LENS = String.join("\n",
            "uniform shader content;",
            "uniform float2 uC;",
            "uniform float2 uHalf;",
            "uniform float uDp;",
            "layout(color) uniform half4 uBg;",
            "uniform float uAlpha;",
            "",
            "float capsule(float2 p, float2 hs) {",
            "  float r = hs.y;",
            "  float2 q = float2(max(abs(p.x) - (hs.x - r), 0.0), p.y);",
            "  return length(q) - r;",
            "}",
            "",
            "half3 under(float2 s) {",
            "  half4 c = content.eval(s);",
            "  return c.rgb + uBg.rgb * (1.0 - c.a);",
            "}",
            "",
            "half4 main(float2 p) {",
            "  float2 q = p - uC;",
            "  float d = capsule(q, uHalf);",
            "  float cover = clamp(0.5 - d, 0.0, 1.0);",
            "  if (cover <= 0.0) return half4(0.0);",
            "  float2 g = float2(capsule(q + float2(0.5, 0.0), uHalf) - capsule(q - float2(0.5, 0.0), uHalf),",
            "                    capsule(q + float2(0.0, 0.5), uHalf) - capsule(q - float2(0.0, 0.5), uHalf));",
            "  float2 n = normalize(g + float2(1e-5));",
            // Bezel: a quarter circle over the outer 60 % of the half-height; its slope sets the bend.
            "  float x = clamp(-d / (uHalf.y * 0.6), 0.0, 1.0);",
            "  float slope = (1.0 - x) / sqrt(max(1.0 - (1.0 - x) * (1.0 - x), 0.03));",
            "  float disp = uHalf.y * clamp(slope * 0.32, 0.0, 1.1);",
            "  float2 s = uC + q * 0.84 - n * disp;",
            "  float2 split = n * disp * 0.1 + n * 0.6 * uDp * (1.0 - x);",
            "  half3 col = half3(under(s - split).r, under(s).g, under(s + split).b);",
            // Clear glass body: a touch of lift so it reads as a material, not a hole.
            "  col = col * 0.93 + 0.035;",
            "  float rimIn = exp(d / (1.3 * uDp));",
            "  float2 light = normalize(float2(-0.55, -0.83));",
            "  col += half3(1.0) * pow(max(dot(n, light), 0.0), 2.5) * rimIn * 0.85;",
            "  col += half3(1.0) * pow(max(dot(n, -light), 0.0), 3.0) * rimIn * 0.3;",
            "  col += half3(1.0) * 0.12 * exp(d / (0.6 * uDp));",
            "  float a = cover * uAlpha;",
            "  return half4(clamp(col, 0.0, 1.0) * a, a);",
            "}");

    private final float dp;
    private final RuntimeShader lens = new RuntimeShader(LENS);
    /** What lies under the lens (track + fill), recorded each frame and drawn once as is. */
    private final RenderNode under = new RenderNode("SliderTrack");
    /** The same layer seen through the glass shader. */
    private final RenderNode glassNode = new RenderNode("SliderGlass");
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** 0 idle capsule .. 1 glass lens; follows the press state with a barely overshooting spring. */
    private final Spring press = new Spring();
    /** Drawn thumb centre (px); follows the value exactly, its speed drives the stretch. */
    private final Spring thumb = new Spring();
    /** Horizontal stretch from drag speed, 0..1. */
    private final Spring stretch = new Spring();
    private boolean thumbPlaced;
    private long lastFrame;
    /** Android "Remove animations" (animator duration scale 0): critically damped springs, no speed stretch. */
    private boolean reduceMotion;
    private static final long TAP_FLASH_MS = 160;
    /** setPressed(true) came since the last ACTION_DOWN, i.e. a real drag (or immediate press) began. */
    private boolean pressSeen;
    /**
     * A finger is down on a drag that started. Window-focus changes call setPressed(false) mid-drag
     * (another window taking focus), which must not drop the glass while the finger still drags.
     */
    private boolean dragging;
    /** Brief press flash after a tap that never became a drag. */
    private final Runnable tapRelease = () -> {
        if (!isPressed()) press.target = 0f;
        postInvalidateOnAnimation();
    };

    GlassSlider(Context c) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        setThumb(null);
        // The default ColorDrawable ignores level changes, so progress changes would not redraw the thumb.
        setProgressDrawable(new ColorDrawable(Color.TRANSPARENT) {
            @Override
            protected boolean onLevelChange(int level) {
                GlassSlider.this.postInvalidateOnAnimation();
                return true;
            }
        });
        setBackground(null);
        setSplitTrack(false);
        shadow.setColor(0x66000000);
        shadow.setMaskFilter(new BlurMaskFilter(5 * dp, BlurMaskFilter.Blur.NORMAL));
        lens.setFloatUniform("uDp", dp);
        lens.setColorUniform("uBg", Ui.CARD);
    }

    private float valueX() {
        int range = getMax() - getMin();
        float f = range > 0 ? (getProgress() - getMin()) / (float) range : 0f;
        return getPaddingLeft() + f * (getWidth() - getPaddingLeft() - getPaddingRight());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        reduceMotion = Prefs.reduceMotion(getContext());
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(tapRelease);
        super.onDetachedFromWindow();
    }

    /**
     * Only observes: a finger that lifts without a drag having started (SeekBar waits for the touch
     * slop inside a scrolling page) still shows a short press, so a tap on the track is not silent.
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) pressSeen = false;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) dragging = false;
        boolean handled = super.onTouchEvent(e);
        if (action == MotionEvent.ACTION_UP && !pressSeen) {
            press.target = 1f;
            postInvalidateOnAnimation();
            removeCallbacks(tapRelease);
            postDelayed(tapRelease, TAP_FLASH_MS);
        }
        return handled;
    }

    /**
     * SeekBar presses itself at once outside a scrolling container and after the touch slop inside
     * one, so scrolling the page past a slider does not pop the lens.
     */
    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (pressed) {
            pressSeen = true;
            dragging = true;
            reduceMotion = Prefs.reduceMotion(getContext());
        }
        press.target = pressed || dragging ? 1f : 0f;
        postInvalidateOnAnimation();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;

        float target = valueX();
        if (!thumbPlaced) {
            thumb.snap(target);
            thumbPlaced = getWidth() > 0;
        }
        float vx = dt > 0f ? (target - thumb.x) / dt : 0f;
        thumb.snap(target);
        stretch.target = reduceMotion ? 0f : Math.min(1f, Math.abs(vx) / (900f * dp));
        if (reduceMotion) stretch.snap(0f);
        else stretch.step(dt, 30f, 0.7f);
        if (reduceMotion) {
            // No overshoot: the lens grows and shrinks without wobbling.
            press.step(dt, press.target > 0.5f ? 32f : 50f, StrandsMotion.JELLY_ZETA_REDUCED);
        } else if (press.target > 0.5f) {
            // zeta 0.8 overshoots about 1.5 %: press is the softer, slower spring, release the stiffer, quicker one.
            press.step(dt, 32f, 0.8f);
        } else {
            press.step(dt, 50f, 0.8f);
        }

        float cy = getHeight() / 2f;
        float x0 = getPaddingLeft(), x1 = getWidth() - getPaddingRight();
        float th = 6 * dp, tr = th / 2f;
        float x = Math.max(x0, Math.min(x1, thumb.x));
        int vw = getWidth(), vh = getHeight();
        under.setPosition(0, 0, vw, vh);
        RecordingCanvas rc = under.beginRecording();
        paint.setColor(TRACK);
        rc.drawRoundRect(x0 - tr, cy - tr, x1 + tr, cy + tr, tr, tr, paint);
        paint.setColor(FILL);
        rc.drawRoundRect(x0 - tr, cy - tr, Math.max(x0 + tr, x), cy + tr, tr, tr, paint);
        under.endRecording();
        canvas.drawRenderNode(under);

        float p = press.x;
        float s = stretch.x * Math.max(0f, Math.min(1f, p));
        float w = (40 + 27 * p) * dp * (1f + 0.22f * s);
        float h = (24 + 9 * p) * dp * (1f - 0.1f * s);
        float half = h / 2f;

        float glass = Math.max(0f, Math.min(1f, p * 1.6f));
        float white = 1f - glass;
        if (glass < 1f) {
            shadow.setAlpha(Math.round(110 * white));
            canvas.drawRoundRect(x - w / 2, cy - half + 1.5f * dp, x + w / 2, cy + half + 1.5f * dp, half, half, shadow);
            paint.setColor(Color.WHITE);
            paint.setAlpha(Math.round(255 * white));
            canvas.drawRoundRect(x - w / 2, cy - half, x + w / 2, cy + half, half, half, paint);
            paint.setAlpha(255);
        }
        if (glass > 0f) {
            shadow.setAlpha(Math.round(90 * glass));
            canvas.drawRoundRect(x - w / 2, cy - half + 2 * dp, x + w / 2, cy + half + 2 * dp, half, half, shadow);
            lens.setFloatUniform("uC", x, cy);
            lens.setFloatUniform("uHalf", w / 2f, half);
            lens.setFloatUniform("uAlpha", glass);
            glassNode.setPosition(0, 0, vw, vh);
            glassNode.setRenderEffect(RenderEffect.createRuntimeShaderEffect(lens, "content"));
            RecordingCanvas gc = glassNode.beginRecording();
            gc.drawRenderNode(under);
            glassNode.endRecording();
            canvas.drawRenderNode(glassNode);
        }

        boolean moving = !press.settled(0.002f) || !stretch.settled(0.002f);
        if (moving) postInvalidateOnAnimation();
        else if (!isPressed()) lastFrame = 0;
    }
}
