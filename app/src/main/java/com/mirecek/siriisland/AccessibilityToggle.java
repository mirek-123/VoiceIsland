package com.mirecek.siriisland;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and (with WRITE_SECURE_SETTINGS, granted once via Shizuku) writes the system list of enabled
 * accessibility services. HyperOS sometimes unbinds or removes third-party services; this restores ours.
 */
final class AccessibilityToggle {
    private AccessibilityToggle() {}

    static ComponentName component(Context c) {
        return new ComponentName(c, SiriAccessibilityService.class);
    }

    static boolean canSelfEnable(Context c) {
        return c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean isListed(Context c) {
        ComponentName me = component(c);
        for (String s : enabledList(c)) {
            if (me.equals(ComponentName.unflattenFromString(s))) return true;
        }
        return false;
    }

    /**
     * Makes sure the service is enabled and bound. Returns false only when it is off and the app lacks
     * WRITE_SECURE_SETTINGS to turn it on itself.
     */
    static boolean ensureRunning(Context c) {
        boolean listed = isListed(c);
        if (listed && SiriAccessibilityService.running() != null) return true;
        if (!canSelfEnable(c)) return listed;

        ContentResolver cr = c.getContentResolver();
        ComponentName me = component(c);
        List<String> others = new ArrayList<>();
        for (String s : enabledList(c)) {
            if (!me.equals(ComponentName.unflattenFromString(s))) others.add(s);
        }
        String without = TextUtils.join(":", others);
        others.add(me.flattenToString());
        String with = TextUtils.join(":", others);
        if (!listed) {
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, with);
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            return true;
        }
        // Listed but not bound (the system marks a service "crashed" when its process dies and does
        // not rebind it). Removing it and re-adding it later makes the system bind it again; done in
        // one go, the two writes coalesce and nothing happens.
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, without);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, with);
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        }, REBIND_DELAY_MS);
        return true;
    }

    private static final long REBIND_DELAY_MS = 600;
    /** Give the system time to bind the service by itself before deciding it is stuck. */
    private static final long RECOVERY_CHECK_MS = 4000;

    /**
     * After our process was (re)started from outside, checks a little later whether the system
     * actually bound the service, and rebinds it if it is enabled but stuck as "crashed".
     */
    static void recoverLater(Context c) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (SiriAccessibilityService.running() != null || isBound(c)) return;
            ensureRunning(c);
        }, RECOVERY_CHECK_MS);
    }

    /** True when the system currently has our service bound (crashed services are not listed). */
    private static boolean isBound(Context c) {
        android.view.accessibility.AccessibilityManager am =
                c.getSystemService(android.view.accessibility.AccessibilityManager.class);
        String me = component(c).flattenToString();
        for (android.accessibilityservice.AccessibilityServiceInfo info : am.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (me.equals(info.getId())) return true;
        }
        return false;
    }

    private static List<String> enabledList(Context c) {
        String raw = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw.split(":")) {
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
}
