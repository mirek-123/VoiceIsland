package com.mirecek.siriisland;

import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.view.SurfaceControl;

interface IShellService {
    // Reserved transaction code Shizuku calls when the user service is unbound.
    void destroy() = 16777114;

    // Runs one shell command as the Shizuku (shell/ADB) user; returns "exit=<code>\n<stdout+stderr>".
    String exec(String command) = 1;

    // Captures `crop` (display pixels) of display 0 at `scale`, leaving out the `exclude` layer
    // (the island's own window). info[0] receives the ColorSpace id, info[1] is 1 when the frame
    // contains secure layers (shown black in captures).
    HardwareBuffer capture(in Rect crop, float scale, in SurfaceControl exclude, out int[] info) = 2;
}
