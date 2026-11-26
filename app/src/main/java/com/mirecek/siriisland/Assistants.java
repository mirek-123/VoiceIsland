package com.mirecek.siriisland;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.Set;

/** Apps whose windows open the island: the built-in assistants plus any app the user picks. */
final class Assistants {
    /** Xiao Ai's UI process on HyperOS (CN). */
    static final String XIAO_AI = "com.miui.voiceassist";
    /** The Google app: hosts the Gemini overlay and Google Assistant (a voice-interaction window). */
    static final String GOOGLE = "com.google.android.googlequicksearchbox";

    static final Set<String> DEFAULT = Set.of(XIAO_AI, GOOGLE);

    private Assistants() {}

    /**
     * The Google app also owns Search and voice search, which are app windows too. Only its
     * assistant surfaces count: Gemini's floating panel is a transparent activity under
     * {@link #GOOGLE_ASSISTANT_SURFACES} (FloatyActivity, measured on MIUI 14 / Android 13), and the
     * older Assistant uses a voice-interaction window, which accessibility reports with no app type.
     */
    static boolean overlayOnly(String pkg) { return GOOGLE.equals(pkg); }

    /** Package prefix of the Google app's Gemini / Assistant activities ("robin" is Gemini). */
    static final String GOOGLE_ASSISTANT_SURFACES = "com.google.android.apps.search.assistant.surfaces.";

    /** Whether a window-state event's class is one of the Google app's assistant surfaces. */
    static boolean assistantSurface(String pkg, CharSequence className) {
        return GOOGLE.equals(pkg) && className != null && className.toString().startsWith(GOOGLE_ASSISTANT_SURFACES);
    }

    /**
     * Whether window {@code w}, owned by {@code pkg}, should open the island. {@code surface}: the
     * window opened as one of the Google app's assistant activities.
     */
    static boolean matches(Set<String> triggers, String pkg, AccessibilityWindowInfo w, boolean surface) {
        if (pkg == null || !triggers.contains(pkg)) return false;
        return !overlayOnly(pkg) || surface || w.getType() != AccessibilityWindowInfo.TYPE_APPLICATION;
    }

    /** Short display name: "Xiao Ai", "Gemini", or the app's own label. */
    static String label(Context c, String pkg) {
        if (XIAO_AI.equals(pkg)) return "Xiao Ai";
        if (GOOGLE.equals(pkg)) return "Gemini";
        try {
            PackageManager pm = c.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    static boolean installed(Context c, String pkg) {
        try {
            c.getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
