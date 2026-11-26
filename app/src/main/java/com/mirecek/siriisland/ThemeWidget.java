package com.mirecek.siriisland;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

/** 2×2 widget: the island in the current theme, with arrows that switch to the previous/next theme. */
public final class ThemeWidget extends AppWidgetProvider {
    static final String ACTION_PREV = "com.mirecek.siriisland.widget.THEME_PREV";
    static final String ACTION_NEXT = "com.mirecek.siriisland.widget.THEME_NEXT";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        IslandWidgets.updateAll(c, goAsync()::finish);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (ACTION_PREV.equals(action) || ACTION_NEXT.equals(action)) {
            int n = Themes.ALL.length;
            int i = IslandWidgets.indexOf(Prefs.theme(c)) + (ACTION_NEXT.equals(action) ? 1 : -1);
            // commit(): the process may end right after onReceive returns.
            Prefs.get(c).edit().putString(Prefs.THEME, Themes.ALL[(i + n) % n].id).commit();
            // The running service redraws every widget on the pref change; without it, redraw here.
            if (SiriAccessibilityService.running() == null) IslandWidgets.updateAll(c, goAsync()::finish);
            return;
        }
        super.onReceive(c, intent);
    }
}
