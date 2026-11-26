package com.mirecek.siriisland;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlendMode;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

import java.util.Arrays;

/**
 * Draws the Siri island: the React Bits Strands orb ({@link StrandsGlassShader}) or the optional 3D
 * glass ({@link FluidLensShader}), morphing out of the camera punch-hole (progress 0) into the
 * expanded island or orb (progress 1). Layers: shadow, glass (refracted wallpaper, body, light,
 * rim), camera patch, glow inside and below. Geometry is in this view's pixels.
 * Spec: docs/superpowers/specs/2026-11-17-strands-bubble-design.md.
 */
final class IslandView extends View {
    interface LevelSource {
        /** Current voice energy, 0..1. Called once per frame, before the other methods. */
        float level();

        /** Energy of bass, low-mid, high-mid and treble, 0..1. */
        default void bands(float[] out) {
            float l = level();
            for (int i = 0; i < 4; i++) out[i] = l;
        }

        /** Xiao Ai stopped listening and has not answered yet. */
        default boolean thinking() { return false; }
    }

    /** Expanded height / width: island 0.755, orb round. */
    static float aspect(boolean orb) { return orb ? 1f : 0.755f; }

    private static final String TAG = "SiriIsland";
    /** Glow level of the default slider position (Prefs.glow with 35 % = 0.7) = Strands glow 1. */
    private static final float DEFAULT_GLOW = 0.7f;
    /** Ease-out for open, close and the close fade (no overshoot). */
    private static final PathInterpolator EASE_OUT = new PathInterpolator(0.23f, 1f, 0.32f, 1f);
    /** CSS {@code ease}: the Reduce Motion fades and the light crossfade. */
    private static final PathInterpolator EASE = new PathInterpolator(0.25f, 0.1f, 0.25f, 1f);
    /** React Bits Strands' intro curve. */
    private static final android.animation.TimeInterpolator INTRO_EASE = StrandsMotion::introEase;

    /** Bound as {@code content} where a shader instance never samples it. */
    private final BitmapShader noContent = new BitmapShader(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    private final RuntimeShader lensEffect = new RuntimeShader(StrandsGlassShader.SOURCE);
    private final RuntimeShader lensSolid = new RuntimeShader(StrandsGlassShader.SOURCE);
    private final RuntimeShader lightOnly = new RuntimeShader(StrandsGlassShader.SOURCE);
    /** Compiled on first use of 3D glass. */
    private RuntimeShader fluidEffect;
    private RuntimeShader fluidSolid;

    private final Paint solidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bloomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bloomLayerPaint = new Paint();
    private final Paint bloomBelowPaint = new Paint();
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RenderNode glassNode = new RenderNode("SiriGlass");
    private final RenderNode bloomNode = new RenderNode("SiriBloom");
    private final RenderNode bloomBelowNode = new RenderNode("SiriBloomBelow");
    private final Path path = new Path();
    private final float dp;

    private float holeCx, holeCy, holeD;
    private boolean hasCamera;
    private float expandedW, expandedH, expandedTop;
    private float progress;
    private LevelSource levelSource;
    private ValueAnimator morphAnim;
    private long lastFrameMs;

    // Look (applyLook).
    private final float[] colors = new float[21];
    private final float[] theme = new float[12];
    private float spectral = 1f;
    private boolean strands = true;
    /** Classic style for WaveStyles: 0 prism, 1 classic, 2 bubble, 3 iOS 27. */
    private float style = 3f;
    private boolean orb;
    private float wavy = 1f;
    /** Strands reaction slider (Prefs.STRANDS_REACTION): scales the voice that drives the Strands wave. */
    private float reaction = 1f;
    /** React Bits Strands props from the settings (applyLook); 1 = the React Bits default. */
    private float sCount = 3f, sAmplitude = 1f, sSpread = 1f, sThickness = 1f, sTaper = 1f;
    private float sIntensity = 1f, sSaturation = 1f, sHue, sSpeed = 1f, sEnergy = StrandsMotion.REST;
    private float refraction = GlassProfile.REFRACTION, dispersion = GlassProfile.DISPERSION, rim = GlassProfile.RIM;
    private boolean stretchReact = true;
    /** Jelly reaction: Strands' lean toward the pull, stretch 0..1, and its click burst. */
    private final float[] lean = new float[2];
    private float stretch, burst;
    private static final float LEAN_TAU_S = 0.12f;
    private static final float BURST_TAU_S = 0.45f;
    private float glow = DEFAULT_GLOW;
    private boolean thinkingAnim = true;
    private boolean liquid = true;
    private boolean glass3d;
    private boolean reduceMotion;
    private float frost;
    private float body = GlassProfile.BODY_CLEAR;
    /** Eased value the shader and patch see. */
    private float light;
    /** Light crossfade: restarts from the current value on each target change, eased the same way both directions. */
    private float lightFrom, lightTo, lightU = 1f;
    private float lightTarget;
    private boolean lightSet;

    // Motion.
    private final StrandsMotion motion = new StrandsMotion();
    private final float[] motionVec = new float[3];
    private final float[] bands = new float[4];
    private float intro;
    private float introFrom, introTo;
    private long introStartMs, introMs;
    private android.animation.TimeInterpolator introInterp = INTRO_EASE;
    private Runnable introDone;
    private Runnable postedIntroDone;
    private boolean introPending;
    private float pulse;
    private float thinkT;
    /** Thinking layer (StrandsLight.thinkGlow): eased 0..1 amount and its own clock in seconds. */
    private float thinkAmt, thinkClock;
    private static final float THINK_FADE_S = 0.25f;
    /** Reduce Motion: the comets drift instead of orbiting. */
    private static final float THINK_CLOCK_REDUCED = 0.35f;
    /** Strands' click pulse spring {x, v} (StrandsMotion.stepClick); swells the orb by PULSE_SWELL × x. */
    private final float[] click = new float[2];
    /** Strands' hover (grab) energy, eased 0.25 s in and 0.6 s out like its pointer hover. */
    private float hover;
    private static final float PULSE_RELEASE_S = 0.15f;

    // Backdrop.
    private Bitmap backdrop;
    private final Rect backdropRect = new Rect();
    /** Reused destination of the backdrop draw. */
    private final Rect backdropDst = new Rect();
    private final BlurCache frostBlur = new BlurCache(Shader.TileMode.CLAMP);
    private final BlurCache bloomBlur = new BlurCache(Shader.TileMode.DECAL);
    private final BlurCache bloomBelowBlur = new BlurCache(Shader.TileMode.DECAL);

    /** A blur RenderEffect rebuilt only when its radius changes. */
    private static final class BlurCache {
        private final Shader.TileMode mode;
        private float radius = -1f;
        private RenderEffect effect;

        BlurCache(Shader.TileMode mode) { this.mode = mode; }

        RenderEffect get(float r) {
            if (effect == null || r != radius) {
                effect = RenderEffect.createBlurEffect(r, r, mode);
                radius = r;
            }
            return effect;
        }
    }
    private boolean backdropDirty;

    // 3D glass frame budget (spec §1.2).
    private final FrameBudget budget = new FrameBudget();
    private boolean budgetPosted;
    private final Choreographer.FrameCallback budgetTick = new Choreographer.FrameCallback() {
        @Override public void doFrame(long nanos) {
            budgetPosted = false;
            if (!glass3d || budget.tripped() || !isAttachedToWindow()) return;
            if (budget.frame(nanos)) {
                Log.w(TAG, "3D glass is over the frame budget; using the Strands lens until the bubble closes");
                invalidate();
                return;
            }
            budgetPosted = true;
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    IslandView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        bloomPaint.setShader(lightOnly);
        bloomLayerPaint.setBlendMode(BlendMode.SCREEN);
        bloomBelowPaint.setBlendMode(BlendMode.SCREEN);
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setMaskFilter(new BlurMaskFilter(GlassProfile.SHADOW_SIGMA_DP * dp, BlurMaskFilter.Blur.NORMAL));
        lensSolid.setInputShader("content", noContent);
        lightOnly.setInputShader("content", noContent);
        setTheme(Themes.ALL[0]);
    }

    void setTheme(Themes.Theme t) {
        for (int i = 0; i < 4; i++) {
            int c = t.colors[i];
            theme[i * 3] = ((c >> 16) & 255) / 255f;
            theme[i * 3 + 1] = ((c >> 8) & 255) / 255f;
            theme[i * 3 + 2] = (c & 255) / 255f;
        }
        System.arraycopy(Themes.strandsColors(t), 0, colors, 0, colors.length);
        spectral = t.spectral ? 1f : 0f;
        invalidate();
    }

    /** Applies every look and glass setting, the system dark mode and Reduce Motion. */
    void applyLook(Context c) {
        setTheme(Prefs.theme(c));
        String st = Prefs.waveStyle(c);
        strands = !Prefs.STYLE_IOS27.equals(st) && !Prefs.STYLE_BUBBLE.equals(st)
                && !Prefs.STYLE_PRISM.equals(st) && !Prefs.STYLE_CLASSIC.equals(st);
        style = Prefs.STYLE_PRISM.equals(st) ? 0f : Prefs.STYLE_CLASSIC.equals(st) ? 1f
                : Prefs.STYLE_BUBBLE.equals(st) ? 2f : 3f;
        orb = Prefs.orb(c);
        wavy = Prefs.waviness(c);
        reaction = Prefs.strandsReaction(c);
        sCount = Prefs.strandsCount(c);
        sAmplitude = Prefs.pct(c, Prefs.STRANDS_AMPLITUDE, 100);
        sSpread = Prefs.pct(c, Prefs.STRANDS_SPREAD, 100);
        sThickness = Prefs.pct(c, Prefs.STRANDS_THICKNESS, 100);
        sTaper = Math.max(0.05f, Prefs.pct(c, Prefs.STRANDS_TAPER, 100));
        sIntensity = Prefs.pct(c, Prefs.STRANDS_INTENSITY, 100);
        sSaturation = Prefs.pct(c, Prefs.STRANDS_SATURATION, 100);
        sHue = Prefs.pct(c, Prefs.STRANDS_HUE, 0);
        sSpeed = Prefs.pct(c, Prefs.STRANDS_SPEED, 100);
        sEnergy = Prefs.strandsEnergy(c);
        refraction = Prefs.pct(c, Prefs.GLASS_REFRACTION, 100);
        dispersion = Prefs.pct(c, Prefs.GLASS_DISPERSION, 100);
        rim = Prefs.pct(c, Prefs.GLASS_RIM, 100);
        stretchReact = Prefs.stretchReact(c);
        glow = Prefs.glow(c);
        thinkingAnim = Prefs.thinkingAnimation(c);
        frost = Prefs.frost(c);
        body = GlassProfile.bodyAlpha(Prefs.tint(c));
        glass3d = Prefs.glass3d(c);
        reduceMotion = Prefs.reduceMotion(c);
        lightTarget = Prefs.night(c) ? 0f : 1f;
        if (!lightSet) {
            light = lightFrom = lightTo = lightTarget;
            lightU = 1f;
            lightSet = true;
        }
        invalidate();
    }

    /** Off: no live wallpaper, the orb's own body. */
    void setLiquid(boolean on) {
        liquid = on;
        invalidate();
    }

    /** The orb is refracting a live backdrop right now. */
    boolean live() { return liquid && backdrop != null; }

    /** New backdrop frame; {@code rect} is where it sits in this view's coordinates. */
    void setBackdrop(Bitmap frame, Rect rect) {
        backdrop = frame;
        backdropRect.set(rect);
        backdropDirty = true;
        invalidate();
    }

    void clearBackdrop() {
        backdrop = null;
        invalidate();
    }

    /** {@code hasCamera}: the display has a real punch-hole at the given position (draws the dark patch over it). */
    void setGeometry(float holeCx, float holeCy, float holeD, float expandedW, float expandedH, float expandedTop,
            boolean hasCamera) {
        this.holeCx = holeCx;
        this.holeCy = holeCy;
        this.holeD = holeD;
        this.expandedW = expandedW;
        this.expandedH = expandedH;
        this.expandedTop = expandedTop;
        this.hasCamera = hasCamera;
        invalidate();
    }

    void setLevelSource(LevelSource source) { levelSource = source; }

    /** Grow out of the camera (no bounce), then Strands' intro once the morph is 60 % done. */
    void open() {
        cancelPostedIntroDone();
        introDone = null;
        introMs = 0;
        introPending = true;
        animateMorph(1f, StrandsMotion.MORPH_OPEN_MS, EASE_OUT, null);
        invalidate();
    }

    /**
     * Fade the strands out, then shrink back into the camera. {@code onClosed} runs on the UI thread.
     * The fade takes as long as the strands are visible: none yet (closing early in the morph) means
     * the shrink starts at once.
     */
    void close(Runnable onClosed) {
        cancelPostedIntroDone();
        introPending = false;
        if (intro <= 0.001f) {
            introMs = 0;
            introDone = null;
            animateMorph(0f, StrandsMotion.MORPH_CLOSE_MS, EASE_OUT, onClosed);
        } else {
            long full = fadeMs(StrandsMotion.INTRO_OUT_MS);
            startIntro(0f, Math.round(full * intro), reduceMotion ? EASE : EASE_OUT,
                    () -> animateMorph(0f, StrandsMotion.MORPH_CLOSE_MS, EASE_OUT, onClosed));
        }
        invalidate();
    }

    /** Fully open without animation (settings preview). */
    void showStatic() {
        cancelPostedIntroDone();
        cancel(morphAnim);
        progress = 1f;
        intro = 1f;
        introMs = 0;
        introDone = null;
        introPending = false;
        invalidate();
    }

    /**
     * Frame-stepped fades follow the system animator duration scale; at scale 0 (Reduce Motion) the
     * fade stays {@code REDUCED_FADE_MS} long, because the fade is what replaces the movement.
     */
    private long fadeMs(long normalMs) {
        if (reduceMotion) return StrandsMotion.REDUCED_FADE_MS;
        return Math.max(1L, Math.round(normalMs * ValueAnimator.getDurationScale()));
    }

    private void startIntro(float to, long ms, android.animation.TimeInterpolator interp, Runnable done) {
        introFrom = intro;
        introTo = to;
        introStartMs = SystemClock.uptimeMillis();
        introMs = Math.max(1, ms);
        introInterp = interp;
        introDone = done;
    }

    /** A finished intro's completion runs posted; a reopen must be able to take it back. */
    private void cancelPostedIntroDone() {
        if (postedIntroDone != null) removeCallbacks(postedIntroDone);
        postedIntroDone = null;
    }

    private float introAt(long now) {
        if (introMs <= 0) return intro;
        float t = Math.min(1f, (now - introStartMs) / (float) introMs);
        if (t >= 1f) {
            introMs = 0;
            Runnable done = introDone;
            introDone = null;
            if (done != null) {
                postedIntroDone = () -> {
                    postedIntroDone = null;
                    done.run();
                };
                post(postedIntroDone);
            }
            return introTo;
        }
        float e = introInterp.getInterpolation(t);
        return introFrom + (introTo - introFrom) * e;
    }

    /** Duration is scaled by the distance left, so reversing mid-morph takes only the remaining way. */
    private void animateMorph(float to, long ms, android.animation.TimeInterpolator interp, Runnable end) {
        cancel(morphAnim);
        morphAnim = ValueAnimator.ofFloat(progress, to);
        morphAnim.setDuration(Math.max(1L, Math.round(ms * Math.abs(to - progress))));
        morphAnim.setInterpolator(interp);
        morphAnim.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        if (end != null) {
            morphAnim.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;
                @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
                @Override public void onAnimationEnd(Animator animation) { if (!cancelled) end.run(); }
            });
        }
        morphAnim.start();
    }

    private static void cancel(ValueAnimator a) {
        if (a != null) a.cancel();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelPostedIntroDone();
        cancel(morphAnim);
        Choreographer.getInstance().removeFrameCallback(budgetTick);
        budgetPosted = false;
        budget.reset();
        super.onDetachedFromWindow();
    }

    // Jelly: the bubble can be grabbed and pulled; it stretches along the pull and narrows across
    // it (volume kept), and on release a spring (StrandsMotion.JELLY_*) brings it back with 1-2 swings.
    /** Rubber-band limit of the pull, as a fraction of the bubble height. */
    private static final float JELLY_REACH = 0.55f;
    /** How far the bubble body follows the finger (the rest becomes stretch). */
    private static final float JELLY_FOLLOW = 0.45f;
    /** Stretch per unit of pull / bubble height. */
    private static final float JELLY_STRETCH = 0.6f;
    private final Spring jellyX = new Spring();
    private final Spring jellyY = new Spring();
    private final Matrix jellyMatrix = new Matrix();
    private final Matrix jellyInverse = new Matrix();
    /** This frame's jelly transform about the island centre (uJelly of the live glass). */
    private boolean jellyOn;
    private float jellyA = 1f, jellyB = 1f, jellyTx, jellyTy;
    private boolean grabbed;
    private float grabX, grabY;
    private long lastMoveMs;
    /** Pointer that grabbed the bubble; the others are ignored. */
    private int grabPointerId = -1;
    private static final long STALE_MOVE_MS = 80;
    /** Stretch axis: follows the pull while held, frozen on release so overshoot squashes. */
    private float axisX, axisY = 1f;
    private float lastL, lastT, lastW, lastH;
    private long lastJellyMs;

    /**
     * Handles a drag on the bubble (coordinates in this view's pixels). Returns false for a down
     * outside the bubble, so the caller can let the touch through. Pointer down also kicks Strands'
     * click pulse spring and adds its burst of energy.
     */
    boolean touch(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                float slack = 8 * dp;
                if (progress < 0.5f || lastW <= 0f
                        || e.getX() < lastL - slack || e.getX() > lastL + lastW + slack
                        || e.getY() < lastT - slack || e.getY() > lastT + lastH + slack) return false;
                grabbed = true;
                grabPointerId = e.getPointerId(e.getActionIndex());
                if (!reduceMotion) click[1] += StrandsMotion.CLICK_KICK;
                burst = Math.min(1.2f, burst + 0.9f);
                grabX = e.getX() - unband(jellyX.x);
                grabY = e.getY() - unband(jellyY.x);
                lastMoveMs = e.getEventTime();
                jellyX.v = jellyY.v = 0f;
                postInvalidateOnAnimation();
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                if (!grabbed) return false;
                int idx = e.findPointerIndex(grabPointerId);
                if (idx < 0) return true;
                float px = e.getX(idx), py = e.getY(idx);
                float nx = band(px - grabX), ny = band(py - grabY);
                float dt = Math.max(0.004f, (e.getEventTime() - lastMoveMs) / 1000f);
                lastMoveMs = e.getEventTime();
                // Keep the finger's speed so a flick carries into the release.
                jellyX.v = jellyX.v * 0.5f + 0.5f * (nx - jellyX.x) / dt;
                jellyY.v = jellyY.v * 0.5f + 0.5f * (ny - jellyY.x) / dt;
                jellyX.x = nx;
                jellyY.x = ny;
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP -> {
                if (!grabbed) return false;
                if (e.getPointerId(e.getActionIndex()) == grabPointerId) releaseGrab(e.getEventTime());
                return true;
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!grabbed) return false;
                releaseGrab(e.getEventTime());
                return true;
            }
            default -> { return grabbed; }
        }
    }

    /** Ends the drag; a finger that rested for STALE_MOVE_MS before lifting releases without speed. */
    private void releaseGrab(long eventTime) {
        grabbed = false;
        grabPointerId = -1;
        if (eventTime - lastMoveMs > STALE_MOVE_MS) jellyX.v = jellyY.v = 0f;
        postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean mine = touch(e);
        if (mine && e.getActionMasked() == MotionEvent.ACTION_DOWN && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return mine || super.onTouchEvent(e);
    }

    /** Rubber band: pull distance -> displacement, approaching JELLY_REACH x height. */
    private float band(float d) {
        float reach = JELLY_REACH * Math.max(lastH, 1f);
        return d / (1f + Math.abs(d) / reach);
    }

    private float unband(float b) {
        float reach = JELLY_REACH * Math.max(lastH, 1f);
        float m = Math.min(Math.abs(b), reach * 0.99f);
        return Math.signum(b) * m / (1f - m / reach);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        float dt = lastJellyMs == 0 ? 0f : Math.min(0.05f, (now - lastJellyMs) / 1000f);
        lastJellyMs = now;
        if (!grabbed) {
            float zeta = reduceMotion ? StrandsMotion.JELLY_ZETA_REDUCED : StrandsMotion.JELLY_ZETA;
            jellyX.step(dt, StrandsMotion.JELLY_OMEGA, zeta);
            jellyY.step(dt, StrandsMotion.JELLY_OMEGA, zeta);
        }
        float jx = jellyX.x, jy = jellyY.x;
        float mag = (float) Math.hypot(jx, jy);
        if (grabbed && mag > 0.5f) {
            axisX = jx / mag;
            axisY = jy / mag;
        }
        if (lastH <= 0f || (mag < 0.2f && !grabbed && Math.hypot(jellyX.v, jellyY.v) < 2f)) {
            jellyX.snap(0f);
            jellyY.snap(0f);
            jellyOn = false;
            drawIsland(canvas);
            return;
        }
        // Per-axis pull: positive stretches, the overshoot past rest squashes; volume kept.
        float sx = Math.max(0.8f, Math.min(1.5f, 1f + JELLY_STRETCH * jx * axisX / lastH));
        float sy = Math.max(0.8f, Math.min(1.5f, 1f + JELLY_STRETCH * jy * axisY / lastH));
        float a = sx / (float) Math.sqrt(sy);
        float b = sy / (float) Math.sqrt(sx);
        float cx = lastL + lastW / 2f, cy = lastT + lastH / 2f;
        jellyMatrix.setScale(a, b, cx, cy);
        jellyMatrix.postTranslate(JELLY_FOLLOW * jx, JELLY_FOLLOW * jy);
        jellyMatrix.invert(jellyInverse);
        jellyOn = true;
        jellyA = a;
        jellyB = b;
        jellyTx = JELLY_FOLLOW * jx;
        jellyTy = JELLY_FOLLOW * jy;
        int save = canvas.save();
        canvas.concat(jellyMatrix);
        drawIsland(canvas);
        canvas.restoreToCount(save);
        postInvalidateOnAnimation();
    }

    /** Voice, thinking, intro, light crossfade and pulses for this frame. */
    private void stepState(long now, float dt) {
        float voice = levelSource != null ? levelSource.level() : 0.35f;
        if (levelSource != null) levelSource.bands(bands);
        else Arrays.fill(bands, voice);
        boolean thinking = thinkingAnim && levelSource != null && levelSource.thinking();
        // Jelly: Strands' lean follows the pull and the stretch adds energy; a grab counts as its hover.
        float reach = JELLY_REACH * Math.max(lastH, 1f);
        float jx = stretchReact ? jellyX.x : 0f, jy = stretchReact ? jellyY.x : 0f;
        stretch = Math.min(1f, (float) Math.hypot(jx, jy) / reach);
        float lx = clamp(jx / Math.max(lastW * 0.5f, 1f), -1.2f, 1.2f) * 0.8f;
        float ly = clamp(-jy / Math.max(lastH * 0.5f, 1f), -1.2f, 1.2f) * 0.8f;
        float k = 1f - (float) Math.exp(-dt / LEAN_TAU_S);
        lean[0] += (lx - lean[0]) * k;
        lean[1] += (ly - lean[1]) * k;
        burst *= (float) Math.exp(-dt / BURST_TAU_S);
        hover += ((grabbed ? 1f : 0f) - hover) * (1f - (float) Math.exp(-dt / (grabbed && hover < 1f ? 0.25f : 0.6f)));
        float boost = 0.18f * hover + burst + 0.3f * stretch;
        motion.step(dt, strands ? voice * reaction : voice, thinking, reduceMotion, sEnergy, sSpeed, boost);
        motion.motion(motionVec);

        if (introPending && progress >= StrandsMotion.INTRO_START) {
            introPending = false;
            startIntro(1f, fadeMs(StrandsMotion.INTRO_MS), reduceMotion ? EASE : INTRO_EASE, null);
        }
        intro = introAt(now);

        // Restart from the current value on a new target (like a CSS transition), so both directions ease out.
        float scale = ValueAnimator.getDurationScale();
        float lightMs = StrandsMotion.LIGHT_FADE_MS * (scale > 0f ? scale : 1f);
        if (lightTarget != lightTo) {
            lightFrom = light;
            lightTo = lightTarget;
            lightU = 0f;
        }
        lightU = Math.min(1f, lightU + dt * 1000f / lightMs);
        light = lightFrom + (lightTo - lightFrom) * EASE.getInterpolation(lightU);

        if (thinking) thinkT += dt;
        else thinkT = 0f;
        thinkAmt += ((thinking ? 1f : 0f) - thinkAmt) * Math.min(1f, dt / THINK_FADE_S);
        if (thinkAmt > 0.002f) thinkClock += dt * (reduceMotion ? THINK_CLOCK_REDUCED : 1f);
        else thinkClock = 0f;
        // The thinking swell eases out on its own when thinking ends.
        float p = pulse + (0f - pulse) * Math.min(1f, dt / PULSE_RELEASE_S);
        if (!reduceMotion && thinking) p = Math.max(p, StrandsMotion.thinkPulse(thinkT));
        pulse = p;
        StrandsMotion.stepClick(click, dt);
    }

    private void drawIsland(Canvas canvas) {
        if (progress <= 0.001f || holeD <= 0f) return;
        long now = SystemClock.uptimeMillis();
        float dt = lastFrameMs == 0 ? 0f : Math.min(0.05f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;
        stepState(now, dt);

        // Width leads height slightly so the hole stretches into a capsule before it drops down.
        float pw = progress;
        float ph = progress < 1f ? progress * progress * (3f - 2f * progress) : progress;
        float w = lerp(holeD, expandedW, pw);
        float h = lerp(holeD, expandedH, ph);
        if (w < h) w = h;
        float top = lerp(holeCy - holeD / 2f, expandedTop, ph);
        // Strands' intro (94 % -> 100 %) and pulses (+3 %) scale the orb about its centre.
        float s = (reduceMotion ? 1f : 0.94f + 0.06f * intro) * (1f + StrandsMotion.PULSE_SWELL * (pulse + click[0]));
        float cy = top + h / 2f;
        w *= s;
        h *= s;
        top = cy - h / 2f;
        float left = holeCx - w / 2f;
        lastL = left;
        lastT = top;
        lastW = w;
        lastH = h;
        float n = orb ? 2f : 2f + 0.4f * Math.max(0f, Math.min(1f, progress));
        boolean use3d = glass3d && !budget.tripped();
        path.rewind();
        if (orb || use3d) {
            // FluidLensShader's SDF is a capsule (a circle for the orb), so clip, shadow and bloom match it.
            path.addRoundRect(left, top, left + w, top + h, h / 2f, h / 2f, Path.Direction.CW);
        } else {
            squircle(path, left, top, w, h, n);
        }

        if (use3d) ensureFluid();
        runBudget(use3d);

        drawShadow(canvas);
        if (live()) {
            drawLive(canvas, left, top, w, h, n, use3d);
        } else {
            RuntimeShader solid = use3d ? fluidSolid : lensSolid;
            configure(solid, -left, -top, w, h, false);
            if (use3d) configureFluid(solid, h); else configureLens(solid, n, false);
            solidPaint.setShader(solid);
            canvas.drawPath(path, solidPaint);
        }
        drawCameraPatch(canvas);
        drawBloom(canvas, left, top, w, h, n);
        postInvalidateOnAnimation();
    }

    private void ensureFluid() {
        if (fluidEffect != null) return;
        fluidEffect = new RuntimeShader(FluidLensShader.SOURCE);
        fluidSolid = new RuntimeShader(FluidLensShader.SOURCE);
        fluidSolid.setInputShader("content", noContent);
    }

    private void runBudget(boolean use3d) {
        if (use3d && !budgetPosted) {
            budgetPosted = true;
            Choreographer.getInstance().postFrameCallback(budgetTick);
        }
    }

    /** Uniforms every Strands shader declares (StrandsLight + WaveStyles). */
    private void configure(RuntimeShader s, float offX, float offY, float w, float h, boolean hasBackdrop) {
        s.setFloatUniform("uOffset", offX, offY);
        s.setFloatUniform("uSize", w, h);
        s.setFloatUniform("uLevel", motion.level());
        s.setFloatUniform("uHasBackdrop", hasBackdrop ? 1f : 0f);
        s.setFloatUniform("uBody", body);
        s.setFloatUniform("uLight", light);
        s.setFloatUniform("uIntro", intro);
        s.setFloatUniform("uMotion", motionVec);
        s.setFloatUniform("uThink", thinkAmt);
        s.setFloatUniform("uThinkT", thinkClock);
        s.setFloatUniform("uColors", colors);
        s.setFloatUniform("uSGlow", glow / DEFAULT_GLOW);
        s.setFloatUniform("uStrands", strands ? 1f : 0f);
        s.setFloatUniform("uTime", motion.clock);
        s.setFloatUniform("uBands", bands);
        s.setFloatUniform("uStyle", style);
        s.setFloatUniform("uWavy", wavy);
        s.setFloatUniform("uSpectral", spectral);
        s.setFloatUniform("uGlow", glow);
        s.setFloatUniform("uCenter", orb ? 0.62f : 0.6f);
        s.setFloatUniform("uP0", theme[0], theme[1], theme[2]);
        s.setFloatUniform("uP1", theme[3], theme[4], theme[5]);
        s.setFloatUniform("uP2", theme[6], theme[7], theme[8]);
        s.setFloatUniform("uP3", theme[9], theme[10], theme[11]);
        s.setFloatUniform("uCount", sCount);
        s.setFloatUniform("uAmplitude", sAmplitude);
        s.setFloatUniform("uSpread", sSpread);
        s.setFloatUniform("uThickness", sThickness);
        s.setFloatUniform("uTaper", sTaper);
        s.setFloatUniform("uIntensity", sIntensity);
        s.setFloatUniform("uSaturation", sSaturation);
        s.setFloatUniform("uHueShift", sHue);
        s.setFloatUniform("uLean", lean);
        s.setFloatUniform("uStretch", stretch);
        // Pulling the jelly makes the glass catch more light and split more colour at the rim.
        s.setFloatUniform("uRim", rim * (1f + 0.6f * stretch));
        s.setFloatUniform("uDispersion", dispersion * (1f + 1.5f * stretch));
        // Drawn under the canvas jelly matrix: no extra stretch in the shader (drawLive overrides).
        s.setFloatUniform("uJelly", 1f, 1f, 0f, 0f);
    }

    private void configureLens(RuntimeShader s, float n, boolean lightOnlyMode) {
        s.setFloatUniform("uN", n);
        s.setFloatUniform("uLightOnly", lightOnlyMode ? 1f : 0f);
        // ...and bend the light harder (Strands' refraction is capped at 1.8).
        s.setFloatUniform("uRefraction", Math.min(1.8f, refraction * (1f + 0.5f * stretch)));
    }

    private void configureFluid(RuntimeShader s, float h) {
        float edge = GlassProfile.FLUID_EDGE * h;
        s.setFloatUniform("uThick", edge);
        s.setFloatUniform("uZ", 2f * edge + GlassProfile.FLUID_GAP * h);
        // Fluid Glass's index of refraction is 1 + 0.5 x refraction (FLUID_IOR at the default).
        s.setFloatUniform("uIor", 1f + 0.5f * Math.min(1.8f, refraction * (1f + 0.5f * stretch)));
    }

    private static final int SQUIRCLE_STEPS = 96;
    /** Soft dark patch hiding the camera lens; radius as a multiple of the punch-hole diameter. */
    private static final float CAMERA_PATCH_SCALE = 1.7f;
    /** On the light orb the patch only darkens the lens edge: smaller radius, slightly weaker centre. */
    private static final float CAMERA_PATCH_SCALE_LIGHT = 0.75f;
    private static final float CAMERA_PATCH_ALPHA_LIGHT = 0.85f;
    private final Paint cameraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix cameraMatrix = new Matrix();
    private android.graphics.RadialGradient cameraShader;

    private void drawCameraPatch(Canvas canvas) {
        if (!hasCamera) return;
        if (cameraShader == null) {
            cameraShader = new android.graphics.RadialGradient(0, 0, 1,
                    new int[]{0xF5000000, 0xE0000000, 0x00000000}, new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP);
            cameraPaint.setShader(cameraShader);
        }
        float radius = holeD * lerp(CAMERA_PATCH_SCALE, CAMERA_PATCH_SCALE_LIGHT, light);
        cameraPaint.setAlpha(Math.round(255 * lerp(1f, CAMERA_PATCH_ALPHA_LIGHT, light)));
        cameraMatrix.setScale(radius, radius);
        cameraMatrix.postTranslate(holeCx, holeCy);
        cameraShader.setLocalMatrix(cameraMatrix);
        canvas.save();
        canvas.clipPath(path);
        canvas.drawCircle(holeCx, holeCy, radius, cameraPaint);
        canvas.restore();
    }

    /** Superellipse |x/a|^n + |y/b|^n = 1 (n = 2 ellipse, ≈ 2.4 the island). */
    private static void squircle(Path out, float left, float top, float w, float h, float n) {
        float a = w / 2f, b = h / 2f, cx = left + a, cy = top + b, e = 2f / n;
        for (int i = 0; i < SQUIRCLE_STEPS; i++) {
            double t = 2 * Math.PI * i / SQUIRCLE_STEPS;
            double c = Math.cos(t), s = Math.sin(t);
            float x = (float) (cx + a * Math.signum(c) * Math.pow(Math.abs(c), e));
            float y = (float) (cy + b * Math.signum(s) * Math.pow(Math.abs(s), e));
            if (i == 0) out.moveTo(x, y); else out.lineTo(x, y);
        }
        out.close();
    }

    /** Captured backdrop → optional frosted blur → Strands lens or 3D glass, clipped to the island. */
    private void drawLive(Canvas canvas, float left, float top, float w, float h, float n, boolean use3d) {
        int cw = backdropRect.width(), ch = backdropRect.height();
        if (backdropDirty) {
            glassNode.setPosition(backdropRect);
            RecordingCanvas rc = glassNode.beginRecording(cw, ch);
            backdropDst.set(0, 0, cw, ch);
            rc.drawBitmap(backdrop, null, backdropDst, bitmapPaint);
            glassNode.endRecording();
            backdropDirty = false;
        }
        RuntimeShader s = use3d ? fluidEffect : lensEffect;
        configure(s, backdropRect.left - left, backdropRect.top - top, w, h, true);
        if (use3d) configureFluid(s, h); else configureLens(s, n, false);
        // The wallpaper must stay where it is on screen while the jelly moves: draw the backdrop
        // without the jelly matrix and let the shader stretch the glass and refract in real time.
        if (jellyOn) s.setFloatUniform("uJelly", jellyA, jellyB, jellyTx, jellyTy);
        RenderEffect glassEffect = RenderEffect.createRuntimeShaderEffect(s, "content");
        float blur = GlassProfile.blurDp(frost) * dp;
        glassNode.setRenderEffect(blur > 0.01f
                ? RenderEffect.createChainEffect(glassEffect, frostBlur.get(blur))
                : glassEffect);
        canvas.save();
        canvas.clipPath(path);
        if (jellyOn) canvas.concat(jellyInverse);
        canvas.drawRenderNode(glassNode);
        canvas.restore();
    }

    /** iOS 27 Regular glass shadow just below the island (outside it). */
    private void drawShadow(Canvas canvas) {
        float strength = intro;
        if (strength <= 0.01f) return;
        shadowPaint.setAlpha(Math.round(255 * GlassProfile.shadowAlpha(light) * strength));
        canvas.save();
        canvas.clipOutPath(path);
        canvas.translate(0f, GlassProfile.SHADOW_OFFSET_DP * dp);
        canvas.drawPath(path, shadowPaint);
        canvas.restore();
    }

    /**
     * Glow: the strands light drawn again into a blurred layer and added back as light inside the
     * island and just below it. Ribbon glow sets the strength.
     */
    private void drawBloom(Canvas canvas, float left, float top, float w, float h, float n) {
        float strength = GlassProfile.BLOOM_STRENGTH * Math.min(1f, glow / DEFAULT_GLOW) * intro;
        if (strength <= 0.01f) return;
        configure(lightOnly, -left, -top, w, h, false);
        configureLens(lightOnly, n, true);
        float radius = GlassProfile.BLOOM_RADIUS * w;
        int pad = Math.round(radius * 2f);
        int nl = Math.round(left) - pad;
        int nt = Math.round(top) - pad;
        int nw = Math.round(w) + 2 * pad;
        int nh = Math.round(h) + 2 * pad;
        bloomNode.setPosition(nl, nt, nl + nw, nt + nh);
        RecordingCanvas rc = bloomNode.beginRecording(nw, nh);
        rc.translate(-nl, -nt);
        rc.drawPath(path, bloomPaint);
        bloomNode.endRecording();
        bloomNode.setRenderEffect(bloomBlur.get(radius));
        bloomLayerPaint.setAlpha(Math.round(255 * Math.min(1f, strength)));
        bloomNode.setUseCompositingLayer(true, bloomLayerPaint);
        canvas.save();
        canvas.clipPath(path);
        canvas.drawRenderNode(bloomNode);
        canvas.restore();

        float shift = GlassProfile.BLOOM_BELOW_SHIFT * h;
        float radiusBelow = GlassProfile.BLOOM_BELOW_RADIUS * w;
        int padBelow = Math.round(radiusBelow * 2f);
        int bl = Math.round(left) - padBelow;
        int bt = Math.round(top) - padBelow;
        int bw = Math.round(w) + 2 * padBelow;
        int bh = Math.round(h + shift) + 2 * padBelow;
        bloomBelowNode.setPosition(bl, bt, bl + bw, bt + bh);
        RecordingCanvas bc = bloomBelowNode.beginRecording(bw, bh);
        bc.translate(-bl, -bt + shift);
        bc.drawPath(path, bloomPaint);
        bloomBelowNode.endRecording();
        bloomBelowNode.setRenderEffect(bloomBelowBlur.get(radiusBelow));
        bloomBelowPaint.setAlpha(Math.round(255 * Math.min(1f, GlassProfile.BLOOM_BELOW_STRENGTH * strength)));
        bloomBelowNode.setUseCompositingLayer(true, bloomBelowPaint);
        canvas.save();
        canvas.clipOutPath(path);
        canvas.clipRect(left - padBelow, top + h * 0.5f, left + w + padBelow, top + h + shift + padBelow);
        canvas.drawRenderNode(bloomBelowNode);
        canvas.restore();
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float clamp(float x, float lo, float hi) { return Math.max(lo, Math.min(hi, x)); }
}
