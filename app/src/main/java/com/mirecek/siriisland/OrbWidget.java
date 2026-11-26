package com.mirecek.siriisland;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** 2×2 widget with no background: a glass orb on the wallpaper; tapping it opens the assistant. */
public final class OrbWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        IslandWidgets.updateAll(c, goAsync()::finish);
    }
}
