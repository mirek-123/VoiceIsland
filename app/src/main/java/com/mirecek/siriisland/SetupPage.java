package com.mirecek.siriisland;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Accessibility service and Shizuku: state, one-tap setup and its log. */
final class SetupPage implements SettingsNav.Page {
    private final MainActivity a;
    private final View root;
    private final TextView a11yStatus;
    private final TextView a11yButton;
    private final TextView shizukuStatus;
    private final TextView shizukuButton;
    private final TextView shizukuLog;
    private boolean setupRunning;

    SetupPage(MainActivity a) {
        this.a = a;
        Ui ui = a.ui();
        LinearLayout col = ui.column();
        LinearLayout g = ui.section(col, null);
        a11yStatus = ui.text("", 13, Ui.SECONDARY, false);
        a11yButton = ui.pill("Enable", this::enableAccessibility);
        ui.add(g, ui.infoRow("Accessibility service", a11yStatus, a11yButton));
        shizukuStatus = ui.text("", 13, Ui.SECONDARY, false);
        shizukuButton = ui.pill("Set up", this::onShizukuClicked);
        ui.add(g, ui.infoRow("Shizuku", shizukuStatus, shizukuButton));
        shizukuLog = ui.text("", 13, 0xFFD1D1D6, false);
        shizukuLog.setTypeface(Typeface.MONOSPACE);
        shizukuLog.setPadding(ui.px(16), 0, ui.px(16), ui.px(12));
        shizukuLog.setVisibility(View.GONE);
        g.addView(shizukuLog);
        ui.footnote(col, "Works alongside Hyper Bridge: while Xiao Ai is open Voice Island covers the cutout, then hands it back.");
        ui.footnote(col, "On HyperOS also set Voice Island to Autostart and Battery saver → No restrictions.");
        root = ui.scroll(col);
        refresh();
    }

    @Override public String title() { return "Setup"; }

    @Override public View view() { return root; }

    @Override public void refresh() {
        boolean listed = AccessibilityToggle.isListed(a);
        boolean running = SiriAccessibilityService.running() != null;
        a11yStatus.setText(running ? "Running" : listed ? "Enabled, waiting for system" : "Off");
        a11yStatus.setTextColor(running ? Ui.SUCCESS : Ui.SECONDARY);
        a11yButton.setVisibility(running ? View.GONE : View.VISIBLE);
        if (setupRunning) return;
        switch (ShizukuSetup.state()) {
            case NOT_RUNNING:
                shizukuStatus.setText(AccessibilityToggle.canSelfEnable(a) ? "Set up ✓ (Shizuku not running)" : "Not running");
                shizukuButton.setText("Open Shizuku");
                break;
            case NEEDS_PERMISSION:
                shizukuStatus.setText("Needs permission");
                shizukuButton.setText("Allow");
                break;
            case READY:
                shizukuStatus.setText(AccessibilityToggle.canSelfEnable(a) ? "Set up ✓" : "Ready");
                shizukuButton.setText(AccessibilityToggle.canSelfEnable(a) ? "Run again" : "Set up");
                break;
        }
    }

    /** Shizuku permission dialog answered. */
    void onPermissionResult(int code, int result) {
        if (code == ShizukuSetup.REQUEST_CODE && result == PackageManager.PERMISSION_GRANTED) runShizukuSetup();
        else refresh();
    }

    private void enableAccessibility() {
        if (AccessibilityToggle.ensureRunning(a) && AccessibilityToggle.isListed(a)) {
            refresh();
            return;
        }
        Toast.makeText(a, "Turn on \"Voice Island\". If it is greyed out, allow restricted settings in App info first.",
                Toast.LENGTH_LONG).show();
        a.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void onShizukuClicked() {
        switch (ShizukuSetup.state()) {
            case NOT_RUNNING:
                Intent launch = a.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                if (launch != null) a.startActivity(launch);
                else a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")));
                break;
            case NEEDS_PERMISSION:
                ShizukuSetup.requestPermission();
                break;
            case READY:
                runShizukuSetup();
                break;
        }
    }

    private void runShizukuSetup() {
        if (setupRunning) return;
        setupRunning = true;
        shizukuButton.setEnabled(false);
        shizukuStatus.setText("Working…");
        shizukuLog.setText("");
        shizukuLog.setVisibility(View.VISIBLE);
        ShizukuSetup.run(a, line -> shizukuLog.append((shizukuLog.length() > 0 ? "\n" : "") + line), () -> {
            setupRunning = false;
            shizukuButton.setEnabled(true);
            refresh();
        });
    }
}
