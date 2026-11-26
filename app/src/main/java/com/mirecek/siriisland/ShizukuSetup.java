package com.mirecek.siriisland;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import java.util.function.Consumer;

import rikka.shizuku.Shizuku;

/**
 * One-tap privileged setup through Shizuku (the same rootless mechanism Hyper Bridge uses).
 * Grants the app WRITE_SECURE_SETTINGS so it can keep its accessibility service alive by itself,
 * grants the audio permission, exempts it from battery restrictions and enables the service.
 */
final class ShizukuSetup {
    static final int REQUEST_CODE = 27;

    private ShizukuSetup() {}

    enum State { NOT_RUNNING, NEEDS_PERMISSION, READY }

    static State state() {
        if (!Shizuku.pingBinder()) return State.NOT_RUNNING;
        if (Shizuku.isPreV11()) return State.NOT_RUNNING;
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                ? State.READY : State.NEEDS_PERMISSION;
    }

    static void requestPermission() {
        Shizuku.requestPermission(REQUEST_CODE);
    }

    /** Runs the setup; {@code log} receives progress lines on the main thread, {@code done} runs last. */
    static void run(Context context, Consumer<String> log, Runnable done) {
        Context app = context.getApplicationContext();
        String pkg = app.getPackageName();
        Handler main = new Handler(Looper.getMainLooper());

        boolean started = ShellClient.request(app, shell -> new Thread(() -> {
            String[][] steps = {
                    {"Grant secure-settings access", "pm grant " + pkg + " android.permission.WRITE_SECURE_SETTINGS"},
                    {"Grant audio meter access", "pm grant " + pkg + " android.permission.RECORD_AUDIO"},
                    {"Exempt from battery optimisation", "cmd deviceidle whitelist +" + pkg},
                    {"Allow background running", "cmd appops set " + pkg + " RUN_ANY_IN_BACKGROUND allow"},
            };
            for (String[] step : steps) {
                String result = exec(shell, step[1]);
                main.post(() -> log.accept(format(step[0], result)));
            }
            main.post(() -> {
                if (AccessibilityToggle.ensureRunning(app)) {
                    log.accept("✓ Enable accessibility service");
                    done.run();
                    return;
                }
                // pm grant can be blocked by Xiaomi's "USB debugging (Security settings)";
                // the shell user can still write the setting directly.
                String list = android.provider.Settings.Secure.getString(app.getContentResolver(),
                        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                String me = AccessibilityToggle.component(app).flattenToString();
                String value = list == null || list.isEmpty() ? me : list + ":" + me;
                new Thread(() -> {
                    String r1 = exec(shell, "settings put secure enabled_accessibility_services '" + value + "'");
                    String r2 = exec(shell, "settings put secure accessibility_enabled 1");
                    main.post(() -> {
                        log.accept(format("Enable accessibility service (shell)", r1.startsWith("exit=0") ? r2 : r1));
                        done.run();
                    });
                }).start();
            });
        }).start());

        if (started) {
            log.accept("Connecting to Shizuku…");
        } else {
            log.accept("✗ Could not start the Shizuku service");
            done.run();
        }
    }

    private static String exec(IShellService shell, String cmd) {
        try {
            return shell.exec(cmd);
        } catch (RemoteException e) {
            return "exit=-1\n" + e;
        }
    }

    private static String format(String label, String result) {
        boolean ok = result.startsWith("exit=0");
        String detail = result.substring(result.indexOf('\n') + 1).trim();
        if (ok) return "✓ " + label;
        if (detail.contains("SecurityException") || detail.contains("not allowed")) {
            detail += "\n   On Xiaomi, turn on Developer options → \"USB debugging (Security settings)\" and retry.";
        }
        return "✗ " + label + (detail.isEmpty() ? "" : "\n   " + detail);
    }
}
