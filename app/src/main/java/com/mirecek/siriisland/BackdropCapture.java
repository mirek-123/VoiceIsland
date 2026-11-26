package com.mirecek.siriisland;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceControl;

import java.util.function.Supplier;

/**
 * Streams the screen region behind the island (with the island itself excluded) from the Shizuku
 * shell service at up to 60 fps while the island is shown. Frames arrive as hardware bitmaps.
 */
final class BackdropCapture {
    private static final String TAG = "SiriIsland";
    /** Capture as fast as the shell returns frames (~10 ms each), up to one per 120 Hz refresh. */
    private static final long FRAME_MS = 8;
    private static final long RETRY_MS = 120;
    private static final long MAX_BACKOFF_MS = 1000;

    interface Sink {
        /** A new backdrop of {@code crop} (display pixels). Main thread. */
        void onBackdrop(Bitmap frame, Rect crop);

        /** No usable frame right now ({@code reason}); show the fallback glass until frames return. Main thread. */
        void onBackdropUnavailable(String reason);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private HandlerThread thread;
    private volatile boolean running;

    /**
     * @param crop    region to capture, in display pixels
     * @param scale   capture resolution relative to the display (1 keeps a clear lens sharp; frosted glass blurs it, so less is enough)
     * @param exclude   supplies the island window's SurfaceControl (read each frame; it can change)
     * @param reconnect run on the main thread when the shell service is gone, to bind it again
     */
    void start(Rect crop, float scale, Supplier<SurfaceControl> exclude, Runnable reconnect, Sink sink) {
        stop();
        running = true;
        thread = new HandlerThread("glass-capture");
        thread.start();
        Handler h = new Handler(thread.getLooper());
        Rect region = new Rect(crop);
        int[] info = new int[2];

        h.post(new Runnable() {
            private int failures;
            private int frames;
            private long busyMs, statStart;

            @Override
            public void run() {
                if (!running) return;
                IShellService shell = ShellClient.current();
                if (shell == null) {
                    // The shell process died (or Shizuku/Shevery restarted): rebind and keep waiting.
                    main.post(reconnect);
                    h.postDelayed(this, RETRY_MS);
                    return;
                }
                SurfaceControl self = exclude.get();
                if (self == null || !self.isValid()) {
                    // Without excluding the island it would capture itself and darken into black.
                    h.postDelayed(this, RETRY_MS);
                    return;
                }
                long t0 = SystemClock.uptimeMillis();
                long wait = FRAME_MS;
                try {
                    HardwareBuffer buffer = shell.capture(region, scale, self, info);
                    if (buffer != null) {
                        failures = 0;
                        if (info[1] == 1) {
                            // Part of the backdrop is a secure window, which the capture blacks out.
                            buffer.close();
                            main.post(() -> { if (running) sink.onBackdropUnavailable("secure window behind the island"); });
                        } else {
                            Bitmap frame = Bitmap.wrapHardwareBuffer(buffer, colorSpaceOf(info[0]));
                            buffer.close();
                            if (frame != null) main.post(() -> { if (running) sink.onBackdrop(frame, region); });
                        }
                    }
                    wait = Math.max(0, FRAME_MS - (SystemClock.uptimeMillis() - t0));
                    frames++;
                    busyMs += SystemClock.uptimeMillis() - t0;
                    if (statStart == 0) statStart = t0;
                    long span = SystemClock.uptimeMillis() - statStart;
                    if (span >= 2000) {
                        Log.d(TAG, "Backdrop capture: " + (frames * 1000 / span) + " fps, "
                                + (busyMs / frames) + " ms per frame");
                        frames = 0;
                        busyMs = 0;
                        statStart = SystemClock.uptimeMillis();
                    }
                } catch (RemoteException | RuntimeException e) {
                    // Transient on HyperOS: the shell server restarting, display transitions, screen
                    // off. Show the fallback glass meanwhile and keep retrying with a backoff.
                    failures++;
                    String reason = e.getMessage() != null ? e.getMessage() : e.toString();
                    if (failures == 1 || failures % 20 == 0) Log.w(TAG, "Backdrop capture failed (" + failures + "x): " + reason);
                    main.post(() -> { if (running) sink.onBackdropUnavailable(reason); });
                    wait = Math.min(MAX_BACKOFF_MS, RETRY_MS * (1L << Math.min(failures, 4)));
                }
                h.postDelayed(this, wait);
            }
        });
    }

    void stop() {
        running = false;
        if (thread != null) {
            thread.quitSafely();
            thread = null;
        }
    }

    private static ColorSpace colorSpaceOf(int id) {
        ColorSpace.Named[] named = ColorSpace.Named.values();
        return id >= 0 && id < named.length ? ColorSpace.get(named[id]) : ColorSpace.get(ColorSpace.Named.SRGB);
    }
}
