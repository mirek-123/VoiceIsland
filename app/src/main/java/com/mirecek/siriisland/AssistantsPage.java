package com.mirecek.siriisland;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Which apps open the island: Xiao Ai, Gemini, and any other installed app. */
final class AssistantsPage implements SettingsNav.Page {
    private final MainActivity a;
    private final View root;

    AssistantsPage(MainActivity a) {
        this.a = a;
        Ui ui = a.ui();
        LinearLayout col = ui.column();
        Set<String> on = Prefs.triggerApps(a);
        PackageManager pm = a.getPackageManager();

        LinearLayout known = ui.section(col, "Assistants");
        addRow(ui, known, pm, Assistants.XIAO_AI, "Xiao Ai", "HyperOS (China)", on);
        addRow(ui, known, pm, Assistants.GOOGLE, "Gemini", "Floating panel · Google Assistant too", on);
        ui.footnote(col, "Only installed apps can be turned on.");

        LinearLayout others = ui.section(col, "Other apps");
        ui.footnote(col, "The island shows whenever the app's window is on screen.");
        root = ui.scroll(col);

        // Icons and labels for every launchable app take a moment; build the list off the main thread.
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            List<ResolveInfo> apps = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
            Map<String, App> byPkg = new LinkedHashMap<>();
            for (ResolveInfo r : apps) {
                String pkg = r.activityInfo.packageName;
                if (pkg.equals(a.getPackageName()) || pkg.equals(Assistants.XIAO_AI)
                        || pkg.equals(Assistants.GOOGLE) || byPkg.containsKey(pkg)) continue;
                byPkg.put(pkg, new App(pkg, r.loadLabel(pm).toString(), r.loadIcon(pm)));
            }
            List<App> list = new ArrayList<>(byPkg.values());
            list.sort(Comparator.comparing((App x) -> !on.contains(x.pkg)).thenComparing(x -> x.label.toLowerCase()));
            main.post(() -> {
                for (App app : list) {
                    ui.add(others, ui.toggleRow(app.icon, app.label, null, on.contains(app.pkg),
                            v -> Prefs.setTrigger(a, app.pkg, v)), 60);
                }
            });
        }, "AssistantsPage").start();
    }

    private record App(String pkg, String label, Drawable icon) {}

    private void addRow(Ui ui, LinearLayout group, PackageManager pm, String pkg, String label, String detail, Set<String> on) {
        Drawable icon = null;
        boolean installed = Assistants.installed(a, pkg);
        if (installed) {
            try {
                icon = pm.getApplicationIcon(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Uninstalled between the two calls: shown without an icon.
            }
        }
        View row = ui.toggleRow(icon, label, installed ? detail : "Not installed", on.contains(pkg),
                v -> Prefs.setTrigger(a, pkg, v));
        if (!installed) {
            row.setAlpha(0.45f);
            row.setEnabled(false);
            ((LinearLayout) row).getChildAt(((LinearLayout) row).getChildCount() - 1).setEnabled(false);
        }
        ui.add(group, row, icon != null ? 60 : 16);
    }

    @Override public String title() { return "Assistants"; }

    @Override public View view() { return root; }

    /** Short summary for the main page row, e.g. "Xiao Ai, Gemini" or "3 apps". */
    static String summary(MainActivity a) {
        Set<String> on = Prefs.triggerApps(a);
        if (on.isEmpty()) return "None";
        if (on.size() > 2) return on.size() + " apps";
        List<String> names = new ArrayList<>();
        for (String pkg : on) names.add(Assistants.label(a, pkg));
        names.sort(null);
        return String.join(", ", names);
    }
}
