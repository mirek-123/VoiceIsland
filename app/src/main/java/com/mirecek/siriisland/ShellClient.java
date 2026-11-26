package com.mirecek.siriisland;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import rikka.shizuku.Shizuku;

/**
 * The single binding to {@link ShellService} through Shizuku, shared by the one-tap setup and the
 * liquid-glass capture. Kept bound while the app runs so the glass is ready the moment Xiao Ai opens
 * (starting the shell process takes a noticeable fraction of a second). All calls on the main thread.
 */
final class ShellClient {
    private ShellClient() {}

    private static IShellService shell;
    private static boolean binding;
    private static long bindingSince;
    /** A bind that has not connected by then (Shizuku died mid-bind) is abandoned and retried. */
    private static final long BIND_TIMEOUT_MS = 3000;
    private static final List<Consumer<IShellService>> waiting = new ArrayList<>();

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            binding = false;
            shell = IShellService.Stub.asInterface(binder);
            List<Consumer<IShellService>> ready = new ArrayList<>(waiting);
            waiting.clear();
            for (Consumer<IShellService> c : ready) c.accept(shell);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            shell = null;
            binding = false;
        }
    };

    private static Shizuku.UserServiceArgs args(Context c) {
        return new Shizuku.UserServiceArgs(new ComponentName(c.getPackageName(), ShellService.class.getName()))
                .daemon(false)
                .processNameSuffix("shell")
                .debuggable(false)
                .version(4);
    }

    /** The live service, or null when not (yet) connected. Safe to read from any thread. */
    static IShellService current() {
        IShellService s = shell;
        return s != null && s.asBinder().isBinderAlive() ? s : null;
    }

    /**
     * Delivers the service to {@code onReady} once connected, binding if needed. Returns false (and
     * never calls back) when Shizuku is not running or not authorised.
     */
    static boolean request(Context context, Consumer<IShellService> onReady) {
        IShellService live = current();
        if (live != null) {
            if (onReady != null) onReady.accept(live);
            return true;
        }
        if (ShizukuSetup.state() != ShizukuSetup.State.READY) return false;
        if (onReady != null) waiting.add(onReady);
        long now = android.os.SystemClock.uptimeMillis();
        if (!binding || now - bindingSince > BIND_TIMEOUT_MS) {
            binding = true;
            bindingSince = now;
            try {
                Shizuku.bindUserService(args(context.getApplicationContext()), CONNECTION);
            } catch (RuntimeException e) {
                binding = false;
                waiting.clear();
                return false;
            }
        }
        return true;
    }
}
