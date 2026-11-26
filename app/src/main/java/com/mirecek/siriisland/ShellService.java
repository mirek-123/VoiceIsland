package com.mirecek.siriisland;

import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.IBinder;
import android.view.SurfaceControl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Shizuku user service: Shizuku starts this class in a separate process running as the shell (ADB)
 * user, so it has ADB privileges without root. Besides shell commands it captures the screen region
 * behind the island (the shell user holds READ_FRAME_BUFFER), which is what lets the island refract
 * and blur the real content behind it.
 *
 * Capture goes through the same framework path as the system screenshot service:
 * IWindowManager.captureDisplay(displayId, CaptureArgs, ScreenCaptureListener). These are hidden
 * APIs, reached by reflection; processes started by Shizuku are not subject to hidden-API limits.
 */
public final class ShellService extends IShellService.Stub {
    private Object windowManager;
    private Method captureDisplay;
    private Method createSyncListener;
    private Method getBuffer;
    private Class<?> builderClass;
    /** Buffer returned by the previous call; already parcelled, so it is released on the next one. */
    private HardwareBuffer lastBuffer;

    public ShellService() {}

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public String exec(String command) {
        try {
            Process p = new ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = p.getInputStream()) {
                byte[] buf = new byte[4096];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            }
            int code = p.waitFor();
            return "exit=" + code + "\n" + out.toString().trim();
        } catch (IOException e) {
            return "exit=-1\n" + e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "exit=-1\ninterrupted";
        }
    }

    @Override
    public synchronized HardwareBuffer capture(Rect crop, float scale, SurfaceControl exclude, int[] colorSpace) {
        try {
            init();
            Object builder = builderClass.getConstructor().newInstance();
            builderClass.getMethod("setSourceCrop", Rect.class).invoke(builder, crop);
            builderClass.getMethod("setFrameScale", float.class).invoke(builder, scale);
            // Include protected (secure) windows when the shell user is allowed to; otherwise they
            // come back black, which we detect below.
            try {
                builderClass.getMethod("setCaptureSecureLayers", boolean.class).invoke(builder, true);
            } catch (ReflectiveOperationException ignored) {
                // Not available on this build; the secure flag below still protects the glass.
            }
            excludeByName(builder);
            if (exclude != null && exclude.isValid()) {
                Object layers = Array.newInstance(SurfaceControl.class, 1);
                Array.set(layers, 0, exclude);
                builderClass.getMethod("setExcludeLayers", layers.getClass()).invoke(builder, layers);
            }
            Object args = builderClass.getMethod("build").invoke(builder);

            Object listener = createSyncListener.invoke(null);
            captureDisplay.invoke(windowManager, 0, args, listener);
            Object shot = getBuffer.invoke(listener);
            if (shot == null) throw new IllegalStateException("capture returned no buffer");

            HardwareBuffer buffer = (HardwareBuffer) shot.getClass().getMethod("getHardwareBuffer").invoke(shot);
            ColorSpace cs = (ColorSpace) shot.getClass().getMethod("getColorSpace").invoke(shot);
            colorSpace[0] = cs != null ? cs.getId() : ColorSpace.Named.SRGB.ordinal();
            // Secure windows (HyperOS marks Xiao Ai's panel this way) are blacked out in captures.
            boolean secure = (boolean) shot.getClass().getMethod("containsSecureLayers").invoke(shot);
            if (colorSpace.length > 1) colorSpace[1] = secure ? 1 : 0;
            if (lastBuffer != null) lastBuffer.close();
            lastBuffer = buffer;
            return buffer;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException(cause.toString());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * Layers left out of the glass by name (HyperOS only): Xiao Ai's windows lay a dark veil and
     * its coloured glow over the whole screen, so without this the glass would show that instead
     * of the wallpaper or app underneath. Names match as substrings of SurfaceFlinger layer names.
     */
    private static final String[] HIDDEN_LAYERS = {"voice_assist_root", "com.miui.voiceassist", "SiriIsland"};
    /** HyperOS CaptureArgs mode in which the layer names are excluded (measured on HyperOS 3). */
    private static final int MODE_EXCLUDE_NAMES = 1;
    private boolean namesUnsupported;

    private void excludeByName(Object builder) {
        if (namesUnsupported) return;
        try {
            builderClass.getMethod("setCaptureMode", int.class).invoke(builder, MODE_EXCLUDE_NAMES);
            builderClass.getMethod("setExcludeOrIncludeLayerNames", String[].class).invoke(builder, (Object) HIDDEN_LAYERS);
        } catch (ReflectiveOperationException e) {
            // Not a HyperOS build: the glass still excludes the island by SurfaceControl.
            namesUnsupported = true;
        }
    }

    private void init() throws ReflectiveOperationException {
        if (windowManager != null) return;
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        IBinder binder = (IBinder) serviceManager.getMethod("getService", String.class).invoke(null, "window");
        Class<?> stub = Class.forName("android.view.IWindowManager$Stub");
        windowManager = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);

        Class<?> screenCapture = Class.forName("android.window.ScreenCapture");
        Class<?> captureArgs = Class.forName("android.window.ScreenCapture$CaptureArgs");
        Class<?> listenerClass = Class.forName("android.window.ScreenCapture$ScreenCaptureListener");
        builderClass = Class.forName("android.window.ScreenCapture$CaptureArgs$Builder");
        createSyncListener = screenCapture.getMethod("createSyncCaptureListener");
        getBuffer = createSyncListener.getReturnType().getMethod("getBuffer");
        captureDisplay = Class.forName("android.view.IWindowManager")
                .getMethod("captureDisplay", int.class, captureArgs, listenerClass);
    }
}
