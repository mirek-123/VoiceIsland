package com.mirecek.siriisland;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.Toast;

import java.util.List;

/**
 * Invisible activity behind the orb widget: opens the assistant and closes. Xiao Ai first (when it
 * is installed and picked on the Assistants page), then the Google app's assistant.
 */
public final class AskActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent target = assistantIntent();
        try {
            if (target == null) throw new ActivityNotFoundException();
            startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "No assistant found. Install Xiao Ai or Gemini.", Toast.LENGTH_LONG).show();
        }
        finish();
        overridePendingTransition(0, 0);
    }

    private Intent assistantIntent() {
        java.util.Set<String> triggers = Prefs.triggerApps(this);
        if (triggers.contains(Assistants.XIAO_AI)) {
            // Xiao Ai's voice-command activity opens its listening panel (measured on HyperOS 3).
            Intent i = resolved(new Intent(Intent.ACTION_VOICE_COMMAND).setPackage(Assistants.XIAO_AI));
            if (i != null) return i;
        }
        if (triggers.contains(Assistants.GOOGLE)) {
            Intent i = resolved(new Intent(Intent.ACTION_ASSIST).setPackage(Assistants.GOOGLE));
            if (i != null) return i;
        }
        return resolved(new Intent(Intent.ACTION_ASSIST));
    }

    /** The intent pinned to its first matching exported activity, or null when nothing handles it. */
    private Intent resolved(Intent i) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo r : list) {
            if (r.activityInfo.exported) return i.setClassName(r.activityInfo.packageName, r.activityInfo.name);
        }
        return null;
    }
}
