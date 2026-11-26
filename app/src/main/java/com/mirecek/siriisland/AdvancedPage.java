package com.mirecek.siriisland;

import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Detection status and Reset to defaults. */
final class AdvancedPage implements SettingsNav.Page {
    private final View root;
    private final TextView detection;

    AdvancedPage(MainActivity a) {
        Ui ui = a.ui();
        LinearLayout col = ui.column();
        LinearLayout g = ui.section(col, "Detection");
        detection = ui.text("", 15, Color.WHITE, false);
        detection.setPadding(ui.px(16), ui.px(14), ui.px(16), ui.px(14));
        detection.setLineSpacing(0, 1.2f);
        g.addView(detection);

        LinearLayout widgets = ui.section(col, "Home-screen widgets");
        ui.add(widgets, ui.switchRow("Idle animation", Prefs.WIDGET_IDLE, false));
        ui.add(widgets, ui.switchRow("Theme change animation", Prefs.WIDGET_THEME_ANIM, true));
        ui.footnote(col, "Idle: the strands drift and the glow breathes. Theme change: the old theme pops out and the new one pops in.");
        col.addView(ui.button("Reset to defaults", Ui.DESTRUCTIVE, a::confirmReset), ui.fullWidth(24));
        ui.footnote(col, "Resets every Voice Island setting. Setup (accessibility, Shizuku) is not affected.");

        LinearLayout credits = ui.section(col, "Credits");
        TextView ct = ui.text("Strands light after React Bits Strands by David Haz (MIT + Commons Clause).\n"
                + "Switch and press feel after Kyant's AndroidLiquidGlass (Apache 2.0).\n"
                + "Shizuku API by RikkaApps (Apache 2.0).\n"
                + "HiddenApiBypass by LSPosed (Apache 2.0).", 15, Color.WHITE, false);
        ct.setPadding(ui.px(16), ui.px(14), ui.px(16), ui.px(14));
        ct.setLineSpacing(0, 1.2f);
        credits.addView(ct);
        ui.footnote(col, "Not affiliated with Apple, Xiaomi or Google. Siri is a trademark of Apple Inc.");
        root = ui.scroll(col);
        refresh();
    }

    @Override public String title() { return "Advanced"; }

    @Override public View view() { return root; }

    @Override public void refresh() {
        String last = SiriAccessibilityService.lastDetection;
        String glass = SiriAccessibilityService.glassStatus;
        detection.setText((last.isEmpty() ? "Waiting for an assistant…" : last) + (glass.isEmpty() ? "" : "\n" + glass));
    }
}
