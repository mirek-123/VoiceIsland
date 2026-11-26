package com.mirecek.siriisland;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After boot or an app update, restore the accessibility service and the watchdog. */
public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            AccessibilityToggle.ensureRunning(context);
            KeepAliveJob.schedule(context);
        }
    }
}
