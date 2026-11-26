package com.mirecek.siriisland;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

/** 2×1 widget: turns the island on or off (the main page's "Show when an assistant opens"). */
public final class SwitchWidget extends AppWidgetProvider {
    static final String ACTION_TOGGLE = "com.mirecek.siriisland.widget.TOGGLE";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        IslandWidgets.updateAll(c, goAsync()::finish);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            // commit(): the process may end right after onReceive returns.
            Prefs.get(c).edit().putBoolean(Prefs.ENABLED, !Prefs.enabled(c)).commit();
            // The running service redraws every widget on the pref change; without it, redraw here.
            if (SiriAccessibilityService.running() == null) IslandWidgets.updateAll(c, goAsync()::finish);
            return;
        }
        super.onReceive(c, intent);
    }
}
