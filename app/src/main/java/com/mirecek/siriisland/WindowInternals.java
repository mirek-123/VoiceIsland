package com.mirecek.siriisland;

import android.util.Log;
import android.view.SurfaceControl;
import android.view.View;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

/**
 * The island window's own SurfaceControl, reached through HiddenApiBypass, so the backdrop capture
 * can leave the island out. Fails soft (returns null) so a ROM that changes these internals only
 * loses the live glass.
 */
final class WindowInternals {
    private static final String TAG = "SiriIsland";

    private WindowInternals() {}

    private static Object viewRoot(View attached) throws Throwable {
        return HiddenApiBypass.invoke(View.class, attached, "getViewRootImpl");
    }

    static SurfaceControl surfaceControl(View attached) {
        try {
            Object root = viewRoot(attached);
            return root == null ? null : (SurfaceControl) HiddenApiBypass.invoke(root.getClass(), root, "getSurfaceControl");
        } catch (Throwable t) {
            Log.w(TAG, "No window SurfaceControl", t);
            return null;
        }
    }
}
