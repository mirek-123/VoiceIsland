package com.mirecek.siriisland;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Main settings page: status card, preview button, the on/off switch and rows into each sub-page. */
final class MainPage implements SettingsNav.Page {
    private final MainActivity a;
    private final View root;
    private final GradientDrawable statusDot = new GradientDrawable();
    private final TextView statusTitle;
    private final TextView statusLine;
    private final TextView fix;
    private final TextView setupValue;
    private final TextView assistantsValue;
    private final ScrollView sv;

    MainPage(MainActivity a) {
        this.a = a;
        Ui ui = a.ui();
        LinearLayout col = ui.column();

        LinearLayout status = ui.section(col, null);
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(ui.px(64));
        row.setPadding(ui.px(16), ui.px(10), ui.px(16), ui.px(10));
        View dot = new View(a);
        statusDot.setShape(GradientDrawable.OVAL);
        dot.setBackground(statusDot);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ui.px(12), ui.px(12));
        dlp.setMarginEnd(ui.px(12));
        row.addView(dot, dlp);
        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        statusTitle = ui.text("", 17, Color.WHITE, true);
        statusLine = ui.text("", 13, Ui.SECONDARY, false);
        texts.addView(statusTitle);
        texts.addView(statusLine);
        row.addView(texts, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        fix = ui.pill("Fix", () -> a.open(a.setupPage()));
        row.addView(fix, new LinearLayout.LayoutParams(Ui.WRAP, ui.px(48)));
        status.addView(row);

        col.addView(ui.button("Preview on screen", Ui.ACCENT, a::previewOnScreen), ui.fullWidth(12));

        LinearLayout show = ui.section(col, null);
        ui.add(show, ui.switchRow("Show when an assistant opens", Prefs.ENABLED, true));
        assistantsValue = ui.value("");
        ui.add(show, ui.navRow(R.drawable.ic_assistant, 0xFFBF5AF2, "Assistants", assistantsValue,
                () -> a.open(a.assistantsPage())));

        LinearLayout look = ui.section(col, null);
        ui.add(look, ui.navRow(R.drawable.ic_look, 0xFF5E5CE6, "Look", null, () -> a.open(a.lookPage())), 60);
        ui.add(look, ui.navRow(R.drawable.ic_wave, Ui.ACCENT, "Waves & glow", null, () -> a.open(a.wavesPage())), 60);
        ui.add(look, ui.navRow(R.drawable.ic_glass, 0xFF64D2FF, "Glass", null, () -> a.open(a.glassPage())), 60);
        ui.add(look, ui.navRow(R.drawable.ic_sound, 0xFFFF375F, "Launch sound", null, () -> a.open(a.soundPage())), 60);

        LinearLayout more = ui.section(col, null);
        setupValue = ui.value("");
        ui.add(more, ui.navRow(R.drawable.ic_setup, Ui.SUCCESS, "Setup", setupValue, () -> a.open(a.setupPage())), 60);
        ui.add(more, ui.navRow(R.drawable.ic_advanced, 0xFF8E8E93, "Advanced", null, () -> a.open(a.advancedPage())), 60);

        sv = ui.scroll(col);
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> {
            if (!a.atRoot()) return;
            a.header().scrollCollapse(y / (float) a.header().collapseRange());
        });
        root = sv;
        refresh();
    }

    /** Header collapse implied by the main page's current scroll (restored when coming back from a sub-page). */
    float collapse() {
        return Math.max(0f, Math.min(1f, sv.getScrollY() / (float) a.header().collapseRange()));
    }

    @Override public String title() { return a.getString(R.string.app_name); }

    @Override public View view() { return root; }

    @Override public void refresh() {
        boolean service = SiriAccessibilityService.running() != null;
        boolean shizuku = ShizukuSetup.state() == ShizukuSetup.State.READY;
        String last = SiriAccessibilityService.lastDetection;
        if (!service) {
            statusDot.setColor(Ui.DESTRUCTIVE);
            statusTitle.setText("Accessibility service off");
            statusLine.setText("Can't see assistants yet");
            fix.setVisibility(View.VISIBLE);
        } else if (!shizuku) {
            statusDot.setColor(Ui.WARNING);
            statusTitle.setText("Ready");
            statusLine.setText("Live glass off · Shizuku not connected");
            fix.setVisibility(View.VISIBLE);
        } else {
            statusDot.setColor(Ui.SUCCESS);
            statusTitle.setText("Ready");
            statusLine.setText(last.isEmpty() ? "Live glass on" : "Live glass on · " + last);
            fix.setVisibility(View.GONE);
        }
        setupValue.setText(service && shizuku ? "All set" : "Needs attention");
        assistantsValue.setText(AssistantsPage.summary(a));
    }
}
