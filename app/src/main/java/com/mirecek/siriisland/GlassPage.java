package com.mirecek.siriisland;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Strands lens or 3D glass, live wallpaper on/off, Clear ↔ Frosted and tint. */
final class GlassPage implements SettingsNav.Page {
    private final MainActivity a;
    private final View root;
    private final Ui.Choice kind;
    private final TextView note;

    GlassPage(MainActivity a) {
        this.a = a;
        Ui ui = a.ui();
        LinearLayout col = ui.column();
        LinearLayout k = ui.section(col, "Glass");
        kind = ui.segmented(Prefs.GLASS_KIND, new String[]{Prefs.GLASS_STRANDS, Prefs.GLASS_3D},
                new String[]{"Strands lens", "3D glass"}, Prefs.glass3d(a) ? Prefs.GLASS_3D : Prefs.GLASS_STRANDS);
        k.addView(kind.view());
        ui.footnote(col, "3D glass switches back to the Strands lens on its own if your phone can't keep up.");
        LinearLayout g = ui.section(col, null);
        ui.add(g, ui.switchRow("Liquid glass", Prefs.LIQUID_GLASS, true));
        ui.add(g, ui.sliderRow("Clear ↔ Frosted", Prefs.GLASS_FROST, 0, 100, 0,
                v -> v == 0 ? "Clear" : v == 100 ? "Frosted" : v + " %", a::previewQuietly));
        ui.add(g, ui.sliderRow("Glass tint", Prefs.TINT_PERCENT, 0, 90, 0, v -> v + " %", a::previewQuietly));
        note = ui.footnote(col, "");
        LinearLayout lens = ui.section(col, "Glass light");
        ui.add(lens, ui.sliderRow("Refraction", Prefs.GLASS_REFRACTION, 0, 180, 100, v -> v + " %", a::previewQuietly));
        ui.add(lens, ui.sliderRow("Colour split", Prefs.GLASS_DISPERSION, 0, 300, 100, v -> v + " %", a::previewQuietly));
        ui.add(lens, ui.sliderRow("Rim", Prefs.GLASS_RIM, 0, 200, 100, v -> v + " %", a::previewQuietly));
        ui.add(lens, ui.switchRow("React to stretch", Prefs.STRETCH_REACT, true));
        ui.footnote(col, "Refraction bends the light around the rim; colour split adds the rainbow edge. With React to stretch on, pulling the bubble tilts the strands toward your finger and makes the rim catch more light.");
        root = ui.scroll(col);
        refresh();
    }

    @Override public String title() { return "Glass"; }

    @Override public View view() { return root; }

    @Override public void refresh() {
        kind.select(Prefs.glass3d(a) ? Prefs.GLASS_3D : Prefs.GLASS_STRANDS);
        String s;
        if (Prefs.highContrastText(a)) s = "Frosted is forced while High contrast text is on.";
        else if (ShizukuSetup.state() != ShizukuSetup.State.READY) s = "Shizuku off: the orb shows its own body, without the wallpaper.";
        else s = "Clear shows the wallpaper bent through the orb; Frosted blurs it.";
        note.setText(s);
    }
}
