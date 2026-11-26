package com.mirecek.siriisland;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;
import java.util.Set;

/**
 * Shows the island while an assistant's window is on screen (Xiao Ai, the Gemini overlay, or any
 * app picked in Assistants).
 *
 * The island is a TYPE_ACCESSIBILITY_OVERLAY window: it sits above the status bar (an app overlay
 * would be drawn underneath the status-bar icons) and needs no "display over other apps" permission.
 */
public final class SiriAccessibilityService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** Apps whose windows open the island ({@link Prefs#triggerApps}). */
    private Set<String> triggers = Assistants.DEFAULT;

    private static final String TAG = "SiriIsland";
    private static final long POLL_MS = 300;
    private static final long PREVIEW_MS = 6000;
    /**
     * Backdrop capture resolution. Mostly clear glass (frost below {@link #SHARP_FROST_BELOW}) shows
     * the lens unblurred, so it needs full resolution; frosted glass blurs the backdrop, where half
     * resolution is indistinguishable.
     */
    private static final float CAPTURE_SCALE_SHARP = 1.0f;
    private static final float CAPTURE_SCALE_FROSTED = 0.5f;
    private static final float SHARP_FROST_BELOW = 0.5f;

    private static SiriAccessibilityService instance;

    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private IslandView island;
    private VoiceActivity voice;
    private boolean attached;
    /** Package of the assistant the island is shown for, or null. */
    private String shownFor;
    private long previewUntil;
    private final BackdropCapture capture = new BackdropCapture();
    private final Rect captureRegion = new Rect();
    private String captureError;

    /** Package of the most recent window-state change; an assistant stays "current" until another app's window replaces it. */
    private String lastStatePackage = "";

    static volatile String lastDetection = "";
    static volatile String glassStatus = "";

    static SiriAccessibilityService running() { return instance; }

    @Override
    protected void onServiceConnected() {
        instance = this;
        wm = getSystemService(WindowManager.class);
        voice = new VoiceActivity(this);
        Prefs.get(this).registerOnSharedPreferenceChangeListener(this);
        triggers = Prefs.triggerApps(this);
        KeepAliveJob.schedule(this);
        // Start the Shizuku shell process now so the glass is ready the moment the assistant opens.
        if (Prefs.liquidGlass(this)) ShellClient.request(this, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg != null && triggers.contains(pkg.toString()) && event.getWindowId() != -1) {
            windowOwners.put(event.getWindowId(), pkg.toString());
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg != null) {
            lastStatePackage = pkg.toString();
            lastStateSurface = Assistants.assistantSurface(lastStatePackage, event.getClassName());
            if (event.getWindowId() != -1 && Assistants.overlayOnly(lastStatePackage)) {
                if (lastStateSurface) surfaceWindows.add(event.getWindowId());
                else surfaceWindows.remove(event.getWindowId());
            }
        }
        refresh();
    }

    @Override
    public void onInterrupt() {}

    /** Dark ↔ light mode switches the orb (spec §1.1). */
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (attached) island.applyLook(this);
    }

    @Override
    public void onDestroy() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(this);
        main.removeCallbacksAndMessages(null);
        if (voice != null) voice.stop();
        detach();
        if (instance == this) instance = null;
        super.onDestroy();
    }

    /**
     * The system disconnected the service (disabled, rebound after an update or a toggle). The object
     * may outlive the connection, and a stale {@link #instance} would make the app believe the
     * service is running while its window token is already dead.
     */
    @Override
    public boolean onUnbind(android.content.Intent intent) {
        main.removeCallbacksAndMessages(null);
        detach();
        if (instance == this) instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (Prefs.TRIGGER_APPS.equals(key)) triggers = Prefs.triggerApps(this);
        if (IslandWidgets.affects(key)) IslandWidgets.updateAll(this, null);
        if (attached) {
            // Only geometry and glass keys restart the backdrop stream; a slider being dragged
            // (look, light, strands) must not stop the live glass on every step.
            boolean geometry = Prefs.SIZE_PERCENT.equals(key) || Prefs.OFFSET_DP.equals(key) || Prefs.SHAPE.equals(key);
            if (geometry) layoutIsland();
            if (geometry || Prefs.LIQUID_GLASS.equals(key)
                    || (Prefs.GLASS_FROST.equals(key) && captureScale() != runningCaptureScale)) {
                applyGlass();
            }
            island.applyLook(this);
        }
        refresh();
    }

    /** Shows the island for a few seconds so the user can judge size and position. */
    void preview() {
        previewUntil = SystemClock.uptimeMillis() + PREVIEW_MS;
        refresh();
    }

    private final Runnable poll = this::refresh;

    private void refresh() {
        main.removeCallbacks(poll);
        String app = Prefs.enabled(this) ? visibleAssistant() : null;
        boolean assistant = app != null;
        boolean preview = SystemClock.uptimeMillis() < previewUntil;
        boolean want = assistant || preview;

        if (!java.util.Objects.equals(app, shownFor)) {
            String label = Assistants.label(this, assistant ? app : shownFor);
            if (label != null && (assistant || shownFor != null)) {
                lastDetection = label + (assistant ? " opened" : " closed") + " at "
                        + android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis());
            }
            if (assistant && shownFor == null) LaunchSound.playIfEnabled(this);
            shownFor = app;
        }

        if (want) {
            if (!show()) return;
            // Taking focus to hide HyperOS's islands is for Xiao Ai only: a settings preview must
            // not steal focus from the settings screen (it would cancel a slider being dragged), and
            // other assistants' panels (Gemini's text box) keep their focus.
            boolean xiaoAi = Assistants.XIAO_AI.equals(app);
            if (xiaoAi && island.getTag() != CLOSING) hideSystemIslands();
            else if (!xiaoAi) restoreSystemIslands();
            updateGlassStatus();
            // Window events are not guaranteed when a floating panel disappears, so re-check while shown.
            main.postDelayed(poll, POLL_MS);
        } else {
            hide();
        }
    }

    private void updateGlassStatus() {
        if (!Prefs.liquidGlass(this)) glassStatus = "Live glass off: the orb's own body";
        else if (island.live()) glassStatus = "Live glass: refracting the wallpaper via Shizuku";
        else glassStatus = "No live wallpaper"
                + (captureError != null ? " (capture failed: " + captureError + ")" : " (Shizuku off)")
                + ": the orb's own body";
    }

    /**
     * Owners of the windows assistants' events came from. Over full-screen apps (video, games)
     * HyperOS often hides a window's content from accessibility (getRoot() returns null), so the
     * package cannot be read from the window itself; its ID, learnt from the app's own events,
     * still identifies it.
     */
    private final java.util.Map<Integer, String> windowOwners = new java.util.HashMap<>();
    /** Google app windows that opened as an assistant surface (Gemini's panel), by window ID. */
    private final java.util.Set<Integer> surfaceWindows = new java.util.HashSet<>();
    /** Whether the most recent window-state change was a Google assistant surface. */
    private boolean lastStateSurface;

    /** Package of the assistant whose window is on screen, or null. */
    private String visibleAssistant() {
        List<AccessibilityWindowInfo> windows = getWindows();
        java.util.Set<Integer> present = new java.util.HashSet<>();
        String found = null;
        for (AccessibilityWindowInfo w : windows) {
            present.add(w.getId());
            AccessibilityNodeInfo root = w.getRoot();
            CharSequence rootPkg = root != null ? root.getPackageName() : null;
            String pkg = rootPkg != null ? rootPkg.toString() : windowOwners.get(w.getId());
            boolean surface = surfaceWindows.contains(w.getId())
                    || (lastStateSurface && pkg != null && pkg.equals(lastStatePackage));
            if (rootPkg != null && triggers.contains(pkg)) windowOwners.put(w.getId(), pkg);
            if (found == null && Assistants.matches(triggers, pkg, w, surface)) found = pkg;
        }
        // Forget windows that have closed (or now belong to another app) so IDs never go stale.
        windowOwners.keySet().retainAll(present);
        surfaceWindows.retainAll(present);
        if (found != null) return found;
        // Fallback for panels the window list does not expose: trust the last window-state change.
        boolean fallback = windows.isEmpty() && triggers.contains(lastStatePackage)
                && !Assistants.overlayOnly(lastStatePackage);
        return fallback ? lastStatePackage : null;
    }

    /** Attaches or re-opens the island; false when this service's connection is gone. */
    private boolean show() {
        if (!attached) {
            island = new IslandView(this);
            island.setLevelSource(voice);
            try {
                wm.addView(island, overlayParams());
            } catch (WindowManager.BadTokenException e) {
                // Connection lost without onUnbind: forget this instance and let the system rebind.
                Log.w(TAG, "Overlay token is dead; rebinding the service", e);
                island = null;
                if (instance == this) instance = null;
                AccessibilityToggle.ensureRunning(getApplicationContext());
                return false;
            }
            attached = true;
            layoutIsland();
            applyGlass();
            island.applyLook(this);
            voice.start();
            island.open();
        } else if (island.getTag() == CLOSING) {
            island.setTag(null);
            island.open();
            placeGrab();
        }
        return true;
    }

    private static final Object CLOSING = new Object();

    private void hide() {
        if (!attached || island.getTag() == CLOSING) return;
        removeGrab();
        island.setTag(CLOSING);
        IslandView closing = island;
        closing.close(() -> {
            if (island == closing && closing.getTag() == CLOSING) detach();
        });
    }

    private void detach() {
        if (!attached) return;
        attached = false;
        removeGrab();
        main.removeCallbacks(regrabFocus);
        restoreSystemIslands();
        capture.stop();
        voice.stop();
        wm.removeViewImmediate(island);
        island = null;
    }

    /**
     * Touch window exactly over the expanded bubble, so it can be pulled and let go (jelly). The
     * island's own full-width window stays untouchable, so touches anywhere else still reach
     * Xiao Ai and the status bar.
     */
    private View grab;
    private final Rect grabRect = new Rect();

    private void placeGrab() {
        if (!attached || grabRect.isEmpty()) return;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                grabRect.width(), grabRect.height(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = grabRect.left;
        lp.y = grabRect.top;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setFitInsetsTypes(0);
        lp.setTitle("SiriIslandGrab");
        if (grab != null) {
            wm.updateViewLayout(grab, lp);
            return;
        }
        View v = new View(this);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int[] loc = new int[2];
        v.setOnTouchListener((view, e) -> {
            if (island == null) return false;
            island.getLocationOnScreen(loc);
            android.view.MotionEvent local = android.view.MotionEvent.obtain(e);
            local.setLocation(e.getRawX() - loc[0], e.getRawY() - loc[1]);
            island.touch(local);
            local.recycle();
            return true;
        });
        try {
            wm.addView(v, lp);
            grab = v;
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not add the bubble touch window", e);
        }
    }

    private void removeGrab() {
        if (grab == null) return;
        View g = grab;
        grab = null;
        try {
            wm.removeViewImmediate(g);
        } catch (RuntimeException e) {
            Log.w(TAG, "Bubble touch window already gone", e);
        }
    }

    /**
     * HyperOS SystemUI temporarily hides every dynamic island while the status bar is hidden (the
     * same path a full-screen camera or video takes); the islands stay posted, so they come back
     * with their own animation and without being re-sent. Only the focused window controls the
     * status bar, so this 1×1 invisible window takes focus while the bubble is open and asks for the
     * status bar to be hidden. A touch outside the bubble or Back hands focus back for a moment, so
     * the touched window (Xiao Ai's text box) can take it and start typing, then focus is taken again;
     * this window is not an IME target, so the keyboard stays with Xiao Ai.
     */
    private View barHider;
    private static final long REGRAB_MS = 600;
    private final Runnable regrabFocus = () -> {
        if (attached && Assistants.XIAO_AI.equals(shownFor) && island.getTag() != CLOSING) hideSystemIslands();
    };

    private void handFocusBack() {
        restoreSystemIslands();
        main.removeCallbacks(regrabFocus);
        main.postDelayed(regrabFocus, REGRAB_MS);
    }

    /** Touches on the keyboard while typing must not hand focus around (the islands would flicker). */
    private boolean keyboardShowing() {
        for (AccessibilityWindowInfo w : getWindows()) {
            if (w.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) return true;
        }
        return false;
    }

    private void hideSystemIslands() {
        if (barHider != null) return;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setFitInsetsTypes(0);
        lp.setTitle("SiriIslandBarHider");
        View v = new View(this);
        v.setFocusable(true);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        v.setOnTouchListener((view, e) -> {
            // Touches on the bubble itself (pull and let go) and on the keyboard keep focus here.
            if (e.getActionMasked() == android.view.MotionEvent.ACTION_OUTSIDE
                    && !grabRect.contains((int) e.getRawX(), (int) e.getRawY())
                    && !keyboardShowing()) {
                handFocusBack();
            }
            return false;
        });
        // targetSdk 36 routes Back through OnBackInvokedDispatcher, not key events. Give focus back
        // first, then replay Back so it reaches whatever was focused before (Xiao Ai, the app).
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                android.window.OnBackInvokedDispatcher d = view.findOnBackInvokedDispatcher();
                if (d == null) return;
                d.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                    handFocusBack();
                    performGlobalAction(GLOBAL_ACTION_BACK);
                });
            }
            @Override public void onViewDetachedFromWindow(View view) {}
        });
        try {
            wm.addView(v, lp);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not add the status-bar window", e);
            return;
        }
        barHider = v;
        android.view.WindowInsetsController c = v.getWindowInsetsController();
        if (c != null) {
            c.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            c.hide(WindowInsets.Type.statusBars());
        }
    }

    private void restoreSystemIslands() {
        if (barHider == null) return;
        View v = barHider;
        barHider = null;
        try {
            wm.removeViewImmediate(v);
        } catch (RuntimeException e) {
            Log.w(TAG, "Status-bar window already gone", e);
        }
    }

    /** Applies glass settings and (re)starts the backdrop stream when liquid glass is on. */
    private void applyGlass() {
        boolean liquid = Prefs.liquidGlass(this);
        island.setLiquid(liquid);
        capture.stop();
        island.clearBackdrop();
        if (!liquid) return;

        IslandView target = island;
        // Start after layout (the window's position and SurfaceControl exist then). The loop itself
        // waits for the shell service and rebinds it whenever Shizuku / Shevery restarts.
        ShellClient.request(this, null);
        target.post(() -> {
            if (island != target || captureRegion.isEmpty()) return;
            int[] loc = new int[2];
            target.getLocationOnScreen(loc);
            Rect display = new Rect(captureRegion);
            display.offset(loc[0], loc[1]);
            float captureScale = captureScale();
            runningCaptureScale = captureScale;
            capture.start(display, captureScale, () -> WindowInternals.surfaceControl(target),
                    () -> ShellClient.request(this, null), new BackdropCapture.Sink() {
                @Override public void onBackdrop(android.graphics.Bitmap frame, Rect crop) {
                    if (island != target) return;
                    Rect local = new Rect(crop);
                    local.offset(-loc[0], -loc[1]);
                    target.setBackdrop(frame, local);
                    captureError = null;
                }
                @Override public void onBackdropUnavailable(String reason) {
                    captureError = reason;
                    if (island == target) target.clearBackdrop();
                }
            });
        });
    }

    /** Capture resolution for the current Clear ↔ Frosted value. */
    private float captureScale() {
        return Prefs.frost(this) < SHARP_FROST_BELOW ? CAPTURE_SCALE_SHARP : CAPTURE_SCALE_FROSTED;
    }

    private float runningCaptureScale = -1f;

    private WindowManager.LayoutParams overlayParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                windowHeight(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setFitInsetsTypes(0);
        lp.setTitle("SiriIsland");
        return lp;
    }

    private int windowHeight() {
        return (int) (getResources().getDisplayMetrics().density * 260);
    }

    /**
     * Bounds of the cutout's real shape inside the bounding rect {@code r} (the rect can be much larger
     * than the lens), or null when the display reports no usable cutout path.
     */
    private static RectF lensBounds(DisplayCutout cutout, Rect r) {
        Path shape = cutout.getCutoutPath();
        if (shape == null || shape.isEmpty()) return null;
        Path inside = new Path(shape);
        Path rect = new Path();
        rect.addRect(new RectF(r), Path.Direction.CW);
        if (!inside.op(rect, Path.Op.INTERSECT) || inside.isEmpty()) return null;
        RectF b = new RectF();
        inside.computeBounds(b, true);
        return b.isEmpty() ? null : b;
    }

    /** Places the island on the camera punch-hole (or top centre when the display has none). */
    private void layoutIsland() {
        WindowMetrics metrics = wm.getCurrentWindowMetrics();
        Rect bounds = metrics.getBounds();
        float dp = getResources().getDisplayMetrics().density;
        WindowInsets insets = metrics.getWindowInsets();
        DisplayCutout cutout = insets.getDisplayCutout();

        float cx = bounds.width() / 2f;
        float statusBar = insets.getInsets(WindowInsets.Type.statusBars()).top;
        float cy = statusBar > 0 ? statusBar / 2f : 14 * dp;
        float hole = Math.max(18 * dp, statusBar * 0.55f);
        boolean hasCamera = false;
        if (cutout != null) {
            for (Rect r : cutout.getBoundingRects()) {
                if (r.top <= 0 && r.width() < bounds.width() / 2) {
                    RectF lens = lensBounds(cutout, r);
                    if (lens != null) {
                        // The real punch-hole shape: the bounding rect can be far larger than the lens.
                        cx = lens.centerX();
                        cy = lens.centerY();
                        hole = Math.min(Math.min(lens.width(), lens.height()), 40 * dp);
                    } else {
                        cx = r.exactCenterX();
                        cy = r.exactCenterY();
                        hole = Math.min(Math.min(r.width(), r.height()), 40 * dp);
                    }
                    hasCamera = true;
                    break;
                }
            }
        }

        float scale = Prefs.scale(this);
        boolean orb = Prefs.orb(this);
        float screenW = Math.min(bounds.width(), bounds.height());
        // iOS 27 iPhone orb (research §6.1): 0.42 of the screen width, top flush with the top of the
        // screen (0.009 W below it), the camera sitting inside the dark cap.
        float w = screenW * (orb ? 0.30f : 0.42f) * scale;
        float h = w * IslandView.aspect(orb);
        float top = orb ? Math.max(2 * dp, cy - hole / 2f - 4 * dp + Prefs.offsetDp(this) * dp)
                : Math.max(0f, screenW * 0.009f + Prefs.offsetDp(this) * dp);
        island.setGeometry(cx, cy, hole, w, h, top, hasCamera);

        // Everything the glass can sample: the island at its overshoot peak, pulled and stretched by
        // the jelly (up to ~1.7x along the pull plus a 0.25 h shift), plus the lens's reach outside.
        float margin = 40 * dp;
        float reachX = w * 0.9f + h * 0.3f + margin;
        captureRegion.set(
                Math.max(0, Math.round(cx - reachX)),
                Math.max(0, Math.round(Math.min(top - h * 0.3f, cy - hole / 2f) - margin)),
                Math.min(bounds.width(), Math.round(cx + reachX)),
                Math.min(bounds.height(), Math.round(top + h * 1.6f + margin)));
        grabRect.set(Math.round(cx - w / 2f), Math.round(top), Math.round(cx + w / 2f), Math.round(top + h));
        placeGrab();
    }
}
