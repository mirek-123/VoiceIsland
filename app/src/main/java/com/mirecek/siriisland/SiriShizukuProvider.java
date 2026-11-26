package com.mirecek.siriisland;

import android.content.Context;

import rikka.shizuku.ShizukuProvider;

/**
 * Shizuku's binder provider, plus self-healing for the accessibility service.
 *
 * When the Shizuku (or Shevery) manager is uninstalled or reinstalled, the permission it defines
 * disappears and Android kills every app that held it ("permissions revoked"). The system then marks
 * our accessibility service as crashed and never rebinds it. The next time the Shizuku server
 * starts, it opens this provider in every client app, which starts our process again; that is the
 * moment to bring the service back.
 */
public final class SiriShizukuProvider extends ShizukuProvider {
    @Override
    public boolean onCreate() {
        boolean created = super.onCreate();
        Context context = getContext();
        if (context != null) AccessibilityToggle.recoverLater(context.getApplicationContext());
        return created;
    }
}
